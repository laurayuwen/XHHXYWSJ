import java.util.HashMap;
import java.util.Scanner;

public class ESFDProcess extends Process {

	public class Pair {
		String majorVal;
		boolean consensus;
		
		public Pair(String theAgreedVal, boolean decision) {
			this.majorVal=theAgreedVal;
			this.consensus=decision;
		}

		String getMajorVal(){
			return majorVal;
		}
		
		boolean getConsensus(){
			return consensus;
		}

	}

	private EventuallyStrongFailureDetector detector;

	private String x;

	private int r = 0;

	private int numOfMsgReceived;
	
	private HashMap<Integer,Pair> roundPairMap;

	

	private HashMap<String, Integer> valAndOccurNumPair = 
			new HashMap<String, Integer>();

	public static final String OUTCOME_TYPE = "OUTCOME";

	public static final String VAL_TYPE = "VAL";

	public ESFDProcess(String name, int pid, int n) {
		super(name, pid, n);

		this.detector = new EventuallyStrongFailureDetector(this);
		
		this.roundPairMap=new HashMap<Integer,Pair>();
	}

	public void begin() {
		this.detector.begin();
	}

	public synchronized void receive(Message m) {
		String type = m.getType();
		if (type != null
				&& type.equals(EventuallyStrongFailureDetector.HeartbeatMessage)) {
			this.detector.receive(m);
		}

		else if (type != null && type.equals(VAL_TYPE)) {
			int senderID = m.getSource();
			String content = m.getPayload();
			int indexOfDelimiter = content.indexOf(',');
			String herVal = content.substring(0, indexOfDelimiter);
			int herRound = Integer.parseInt(content
					.substring(indexOfDelimiter + 1));

			// if the msg received from the process is for the earlier round,
			// then safely ignore it.
			if (herRound != this.r) {
				System.out.println("ignore the msg from P" + senderID
						+ " in round " + herRound);
				return;
			}

			else {

				System.out.println("get the val msg from P" + senderID
						+ " in round " + herRound);

				this.numOfMsgReceived++;


				this.addToValOccurNumPairMap(herVal);

			}
		}

		else if (type != null && type.equals(OUTCOME_TYPE)) {
			int senderID = m.getSource();
			String content = m.getPayload();

			int firstIndexOfDelimiter = content.indexOf(',');
			int lastDelimiterIndex = content.lastIndexOf(',');

			String decisionStr = content.substring(0, firstIndexOfDelimiter);
			boolean decision = decisionStr.equals("true") ? true : false;

			String theAgreedVal = content.substring(firstIndexOfDelimiter + 1,
					lastDelimiterIndex);

			int herRound = Integer.parseInt(content
					.substring(lastDelimiterIndex + 1));

			if (herRound != this.r || senderID!=(this.r % this.n) + 1)
				return;

			else {
				this.roundPairMap.put(this.r, new Pair(theAgreedVal,decision));
			}
		}
	}

	private void addToValOccurNumPairMap(String herVal) {
		// if the val already exists, then update the number of occurences
		// else put the new key value pair in to the map.
		if (this.valAndOccurNumPair.containsKey(herVal))
			this.valAndOccurNumPair.put(herVal,
					this.valAndOccurNumPair.get(herVal) + 1);
		else {
			this.valAndOccurNumPair.put(herVal, 1);
		}
	}

	private String getMajorValFromMap() {
		int max = 0;
		String mostFrequentVal = null;

		for (String val : this.valAndOccurNumPair.keySet()) {
			int occNum = this.valAndOccurNumPair.get(val);
			if (occNum > max) {
				max = occNum;
				mostFrequentVal = val;
			}
		}

		return mostFrequentVal;
	}

	private boolean isConsensusOnSingleVal() {
		return this.valAndOccurNumPair.keySet().size() == 1;
	}

	private int getMaxFailNumber() {
		return (int) Math.floor((this.n - 1) / 3.0);
	}

	public static void main(String[] args) {
		String pName = args[0];
		int id = Integer.parseInt(args[1]);
		int numOfProcesses = Integer.parseInt(args[2]);

		ESFDProcess p = new ESFDProcess(pName, id, numOfProcesses);

		int F = p.getMaxFailNumber();

		// the user is able to give the value of x as an argument of the
		// program.
		if (args.length == 4) {
			p.x = args[3];
		}
	

		p.registeR();
		p.begin();

		if (p.x == null) {
			// if the user did not give the value of x when program starts,
			// she is still able to give it at this point.
			System.out.println("Input a value please:\n");

			Scanner scan = new Scanner(System.in);

			p.x = scan.nextLine();

			scan.close();
		}

		int m = 0;

		while (true) {

			p.r++;

			// c is the coordinator for the cur round
			int c = (p.r % p.n) + 1;


			// if it is not the coordinator of the current round
			// then send the msg of round r to the coordinator of that round.
			if (p.pid != c) {

				String content = p.x + "," + p.r;

				Message msgToCoordinator = new Message();
				msgToCoordinator.setDestination(c);
				msgToCoordinator.setType(VAL_TYPE);
				msgToCoordinator.setPayload(content);
				msgToCoordinator.setSource(p.pid);
				p.unicast(msgToCoordinator);
			}

			// collect the msgs from the others
			// if it is the coordinator of the current round.
			else {
				p.resetVar();
				p.addToValOccurNumPairMap(p.x);

				// the coordinator does not need to receive the msg from itself
				while (p.numOfMsgReceived < p.n - F - 1) {
					Thread.yield();
				}

				// when we escape the loop, we have already got n-F messages
				// (incl. the coordinator's own msg)
				// then we can make a statistics analysis
				String majorVal = p.getMajorValFromMap();

				boolean consensus = p.isConsensusOnSingleVal();

				// send the outcome to each member of the group.
				p.sendToParticipants(majorVal, consensus, p.r);

				// because it is the coordinator, it can collect outcome
				// directly.
				p.x = majorVal;

				if (consensus) {
					System.out.println("Process " + p.pid + " has decided on "
							+ p.x);


					if (m == p.pid) {

						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						// System.exit(0);
						System.out.println("coordinator " + p.getName()
								+ " exits.");
						System.exit(0);
					}

					else if (m == 0) {
						m = p.pid;
						
						System.out.println(p.getName()+":" +
								"I find the consensus val alone");
					}
				}
				
				else{
	
						System.out.println(p.getName()+": No consensus reached"+
								", only get "+p.valAndOccurNumPair.get(majorVal)+
								" votes out of "+(p.n-F)+" for the value '"+majorVal+"'");
					
				}

				p.resetVar();
				continue;
			}


			
			while(!p.detector.isSuspect(c)){
				Pair outcomePair=p.roundPairMap.get(p.r);
			//if this is not null, it indicates we have received outcome message from the coordinator	
			if ( outcomePair!= null) {
				p.x = outcomePair.getMajorVal();

				if (outcomePair.getConsensus()) {
					System.out.println("Process " + p.pid + " has decided "
							+ p.x);

					if (m == c) {
//						p.printAllTheMajorValDecidedByCoordinators();
						// System.exit(0);
						System.out.println(p.getName()
								+ " exits normally after being"
								+ " informed by P" + c);
						
						System.exit(0);
					}

					else if (m == 0) {
						m = c;

						System.out.println("It is P" + c
								+ " who first told me the consensus decision");
					}
				}
				
				break;
			}
			
			else{
				Thread.yield();
			}
		}

		}

	}

	private void resetVar() {
		this.numOfMsgReceived = 0;
		this.valAndOccurNumPair.clear();
	}


	private void sendToParticipants(String majorVal, boolean consensus,
			int round) {
		String content = consensus + "," + majorVal + "," + round;

		this.broadcast(OUTCOME_TYPE, content);

	}

}
