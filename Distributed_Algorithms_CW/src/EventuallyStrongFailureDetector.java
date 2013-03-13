import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;


public class EventuallyStrongFailureDetector implements IFailureDetector{
	static final String HeartbeatMessage="heartbeat";
	
	static final String NULL_MSG="null";
	
	static final int Delta=2000; /* 2second*/
	
	static final int Timeout = Delta+Utils.DELAY;
	
	Process p;
	LinkedList<Integer> suspects;
	Timer bcastTask;
	Timer checkTask;
	long[] link_timeoutArray; 
	long[] timestampForReceivingLastMsg;
	
	
	
	private class PeriodicBroadcastTask extends TimerTask {

		@Override
		public void run() {
			p.broadcast(HeartbeatMessage, String.format("%d", 
						System.currentTimeMillis()));
			
			}
		}
	
	private class PeriodicCheckTask extends TimerTask{

		private EventuallyStrongFailureDetector esfd;
		private long[] theLinkTimeOutArray;
		
		private long[] theRecordedTimeOfLastMsg;
		
		public PeriodicCheckTask(EventuallyStrongFailureDetector epfd){
			this.esfd=epfd;
			this.theLinkTimeOutArray=this.esfd.link_timeoutArray;
			this.theRecordedTimeOfLastMsg=this.esfd.timestampForReceivingLastMsg;
		}
		
		@Override
		public void run() {
			long curTimestamp=System.currentTimeMillis();
			
			for (int i = 1; i < theLinkTimeOutArray.length; i++) {
				if(i==this.esfd.p.pid)
					continue;
				
				long dur=curTimestamp-this.theRecordedTimeOfLastMsg[i];
				if(dur>Delta+theLinkTimeOutArray[i]){
				
					//the timeout value for process i is 
					//Delta+its own max link delay.
					this.esfd.isSuspected(i);
				}
			}
			
		}
		
	}
	
	

	public EventuallyStrongFailureDetector(Process p) {
		super();
		this.p = p;
		this.bcastTask=new Timer();
		this.checkTask=new Timer();
		this.suspects=new LinkedList<Integer>();
		
		init();
	}
	
	

	private void init() {
		int numOfP=this.p.n;
		this.link_timeoutArray=new long[numOfP+1];
		
		//at the beginning, no msg arrived, let the default value be the cur time. 
		this.timestampForReceivingLastMsg=new long[numOfP+1];
		
		long curTime=System.currentTimeMillis();
		
		for (int i = 1; i <= numOfP; i++) {
			//the inital timeout value is set to be 0
			//in order to have the effects that at first the correct 
			//processes suspect each other but finally they won't
			this.link_timeoutArray[i]=0;
			this.timestampForReceivingLastMsg[i]=curTime;
		}
		
	}



	@Override
	public void begin() {
		this.bcastTask.schedule(new PeriodicBroadcastTask(), 0, Delta);

		this.checkTask.schedule(new PeriodicCheckTask(this), 0, Timeout);
	}

	@Override
	public int getLeader() {
		// TODO Auto-generated method stub
		return -1;
	}

	@Override
	public boolean isSuspect(Integer pid) {
		return this.suspects.contains(pid);
	}

	@Override
	public void isSuspected(Integer pid) {
		if(!this.suspects.contains(pid)){
		this.suspects.add(pid);
		
		System.out.println(this.p.pid+": I suspect "+pid);
		}
	}

	@Override
	public void receive(Message m) {
		
		long link_delay=System.currentTimeMillis()-
					Long.parseLong(m.getPayload());
	
		int herPID=m.getSource();
		
//		System.out.println("The link delay of "+herPID+" is "+link_delay+" miliseconds");
		
		this.timestampForReceivingLastMsg[herPID]=System.currentTimeMillis();
		
		removeFromSuspiciousList(herPID);
		
		if(link_delay>this.link_timeoutArray[herPID])
		{
			//if the delay for receiving a specific process is larger than 
			//expected, then modify our assumption accordingly.
			this.link_timeoutArray[herPID]=link_delay;
			
//			System.out.println("Process"+this.p.pid+
//				"Increase the max link delay for Process"+herPID+" to "+link_delay);
		}
		
//		Utils.out(p.pid,m.toString());		

	}



	private void removeFromSuspiciousList(int herPID) {
		
		if(!this.suspects.contains(herPID)){
			return;
		}
		
		for (int i = 0; i < suspects.size(); i++) {
			if(herPID==suspects.get(i)){
				suspects.remove(i);
				break;
			}
		}
		
		System.out.println("Process "+this.p.pid+
				":I don't suspect process"+herPID+" now");
		
	}
}
