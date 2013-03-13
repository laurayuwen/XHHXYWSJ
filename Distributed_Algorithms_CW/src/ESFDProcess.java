import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;


public class ESFDProcess extends Process {
	
	private EventuallyStrongFailureDetector detector;
	
	private String x;
	
	private int r=0;
	
	private int numOfMsgReceived=0;
	
	private String[] majorValCalByProcesses;
	
	private boolean[] decisionsMadeByProcesses;
	
	private ArrayList<Integer> sendersList=new ArrayList<Integer>();
	
	private HashMap<String,Integer> valAndOccurNumPair=new HashMap<String,Integer>();
	
	public static final String OUTCOME_TYPE="OUTCOME";
	
	public static final String VAL_TYPE="VAL";
	
	public ESFDProcess(String name, int pid, int n) {
		super(name, pid, n);
		
		this.detector=new EventuallyStrongFailureDetector(this);
	}
	
	public void begin(){
		this.detector.begin();
	}

	public synchronized void receive(Message m){
		String type=m.getType();
		if(type!=null && type.equals(EventuallyStrongFailureDetector.HeartbeatMessage)){
			this.detector.receive(m);
		}
		
		else if(type!=null && type.equals(VAL_TYPE)){
			int senderID=m.getSource();
			String content=m.getPayload();
			int indexOfDelimiter=content.indexOf(',');
			String herVal=content.substring(0,indexOfDelimiter);
			int herRound=Integer.parseInt(content.substring(indexOfDelimiter+1));
			
			//if the msg received from the process is not for the current round,
			//then safely ignore it.
			if(herRound!=this.r)
				return;
			
			else{
				this.numOfMsgReceived++;
				
				this.sendersList.add(senderID);
				
				this.addToValOccurNumPairMap(herVal);
				
			}
		}
		
		else if(type!=null && type.equals(OUTCOME_TYPE)){
			int senderID=m.getSource();
			String content=m.getPayload();
			
			int firstIndexOfDelimiter=content.indexOf(',');
			int lastDelimiterIndex=content.lastIndexOf(',');
			
			String decisionStr= content.substring(0,firstIndexOfDelimiter);
			boolean decision= decisionStr.equals("true")?true:false;
			
			String theAgreedVal=content.substring(firstIndexOfDelimiter+1,lastDelimiterIndex);
			
			int herRound=Integer.parseInt(content.substring(lastDelimiterIndex+1));
			
			if(herRound!=this.r)
				return;
			
			else{
				this.decisionsMadeByProcesses[senderID]=decision;
				this.majorValCalByProcesses[senderID]=theAgreedVal;
			}
		}
	}
	
	private void addToValOccurNumPairMap(String herVal){
		//if the val already exists, then update the number of occurences
		//else put the new key value pair in to the map.
		if(this.valAndOccurNumPair.containsKey(herVal))
			this.valAndOccurNumPair.put(herVal, this.valAndOccurNumPair.get(herVal)+1);
		else{
			this.valAndOccurNumPair.put(herVal, 1);
		}
	}
	
	private String getMajorValFromMap() {
		int max=0;
		String mostFrequentVal=null;
		
		for (String val : this.valAndOccurNumPair.keySet()) {
			int occNum=this.valAndOccurNumPair.get(val);
			if(occNum>max){
				max=occNum;
				mostFrequentVal=val;
			}
		}
		
		return mostFrequentVal;
	}
	
	private boolean isConsensusOnSingleVal() {
		return this.valAndOccurNumPair.keySet().size()==1;
	}
	
	
	public static void main(String[] args) 
	{		
		String pName=args[0];
		int id=Integer.parseInt(args[1]);
		int n=Integer.parseInt(args[2]);
	
		int F= (int)Math.floor((n-1)/3.0);		
		
		ESFDProcess p=new ESFDProcess(pName,id,n);
		
		//the user is able to give the value of x as an argument of the program.
		if(args.length==4){
			p.x=args[3];
		}
		
		p.majorValCalByProcesses=new String[n+1];
		p.decisionsMadeByProcesses=new boolean[n+1];
		
		//the coordinator needs to compute the outcome,
		//this value is linear to the number of processes.
		int maxCompTime4Coordinator=n*20;
		
		p.registeR();
		p.begin();
		
		if(p.x==null){
		//if the user did not give the value of x when program starts,
		//she is still able to give it at this point.
		System.out.println("Input a value please:\n");
			
		Scanner scan=new Scanner(System.in);
		
		p.x=scan.nextLine();
		
		scan.close();
		}
		
		int m=0;
		
		
		
		
		while (true) {
				
			p.r++;
			
			
			//c is the coordinator for the cur round
			int c= (p.r%n) + 1;
			
			//reset the variables
			reset(p,c);
			
			//if it is not the coordinator of the current round
			//then send the msg of round r to the coordinator of that round.
			if(p.pid!=c){
			
			String content=p.x+","+p.r;
			
			Message msgToCoordinator=new Message();
			msgToCoordinator.setDestination(c);
			msgToCoordinator.setType(VAL_TYPE);
			msgToCoordinator.setPayload(content);
			msgToCoordinator.setSource(p.pid);
			p.unicast(msgToCoordinator);
			}
			
			//collect the msgs from the others if it is the coordinator of the current round.
			else{
				p.addToValOccurNumPairMap(p.x);
				
				//the coordinator does not need to receive the msg from itself
				while(p.numOfMsgReceived < n-F-1){
					Thread.yield();
				}
				
				//when we escape the loop, we have already got n-F messages (incl. the coordinator's own msg)
				//then we can make a statistics analysis
				String majorVal=p.getMajorValFromMap();
				
				boolean consensus=p.isConsensusOnSingleVal();
				
				//send the outcome to each member of the group.
				p.sendToGroupOfParticipants(majorVal,consensus,p.r);
				
				
				//because it is the coordinator, it can collect outcome directly.
				p.x=majorVal;
				
				if(consensus){
					System.out.println("Process "+p.pid+" has decided on "+p.x);
					
					if(m==p.pid){
						p.printAllTheMajorValDecidedByCoordinators();
						
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						System.exit(0);
					}
					
					else if(m==0){
						m=p.pid;
					}
				}
				
				continue;
			}
			
			/////////////////////////////////////////////////////////////////////////////////////////////////////////
			//the max timeout value is calculated by adding 
			//two max_link_delay and the max computation time of the coordinator
			//get the latest link delay from the detector!
			long maxTimeout4Coordinator=2*p.detector.link_timeoutArray[c]+maxCompTime4Coordinator;
			
			try {
				Thread.sleep(maxTimeout4Coordinator);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			if(p.majorValCalByProcesses[c]!=null){
				p.x=p.majorValCalByProcesses[c];
				
				if(p.decisionsMadeByProcesses[c]){
					System.out.println("Process "+p.pid+" has decided "+p.x);
					
					if(m==c){
						p.printAllTheMajorValDecidedByCoordinators();
						System.exit(0);
					}
					
					else if(m==0){
						m=c;
					}
				}
			}
				
		}
		
		
	}

	

	private void printAllTheMajorValDecidedByCoordinators() {
		for (int i = 0; i < this.decisionsMadeByProcesses.length; i++) {
			if(i==this.pid)
				continue;
			
			System.out.println("Process "+i+" : major val is "+this.majorValCalByProcesses[i]+
								", and it has "+(this.decisionsMadeByProcesses[i]?"consensus":"conflict")+" in the voting");
		}
		
	}

	private void sendToGroupOfParticipants(String majorVal, boolean consensus, int round) {
		String content=consensus+","+majorVal+","+round;
		
		for (int i = 0; i < this.sendersList.size(); i++) {
			Message msg=new Message();
			msg.setDestination(this.sendersList.get(i));
			msg.setType(ESFDProcess.OUTCOME_TYPE);
			msg.setPayload(content);
			msg.setSource(this.pid);
			this.unicast(msg);
		}
		
	}

	private static void reset(ESFDProcess p, int cid) {
		p.numOfMsgReceived=0;
		p.sendersList.clear();
		p.valAndOccurNumPair.clear();
		p.decisionsMadeByProcesses[cid]=false;
		p.majorValCalByProcesses[cid]=null;
	}
	
	
}
