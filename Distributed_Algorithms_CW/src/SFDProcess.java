import java.util.Scanner;


public class SFDProcess extends Process {
	private IFailureDetector detector;
	
	public static final String ConsensusMsgType="VAL";
	
	private String x;

	//this array records whether the current process has received a msg of type VAL.
	protected boolean[] hasReceivedValFrom;
	
	//we use this array to hold the msg from other processes
	//because the order of messages may be non-deterministic
	//some processes may go faster and send their values before the other processes are ready to receive.
	//in order to ensure that the value got from process r is only used in round r,
	//we use this array to hold the values received from different processes.
	protected String[] valReceived;
	

	public SFDProcess(String processName, int pid, int n) {
		super(processName, pid, n);
		
		this.hasReceivedValFrom=new boolean[n+1];
		
		this.valReceived=new String[n+1];
		
		this.detector=new StrongFailureDetector(this);
	}
	
	public void begin(){
		this.detector.begin();
	}

	public synchronized void receive(Message m){
		String type=m.getType();
		if(type!=null && type.equals(StrongFailureDetector.HeartbeatMessage)){
			this.detector.receive(m);
		}
		
		else if(type!=null && type.equals(ConsensusMsgType)){
			int senderID=m.getSource();
			
			this.valReceived[senderID]=m.getPayload();
			
			this.hasReceivedValFrom[senderID]=true;
		}
	}
	
	
	public static void main(String[] args) 
	{		
		String pName=args[0];
		int id=Integer.parseInt(args[1]);
		int n=Integer.parseInt(args[2]);
		SFDProcess p=new SFDProcess(pName,id,n);
		p.registeR();
		p.begin();
		
		Scanner scan=new Scanner(System.in);
		p.x=scan.nextLine();
		
		for (int r = 1; r <= n; r++) {
			if(id==r){
				p.broadcast(ConsensusMsgType, p.x);
			}
			
			else{
				while(!p.detector.isSuspect(r)){
					if(p.hasReceivedValFrom[r]){
						p.x=p.valReceived[r];
						break;
					}
					
					else
						Thread.yield();
				}
			}
		}
		
		System.out.println("Process "+p.pid+": I have decided the value "+p.x);
		scan.close();
	}

}
