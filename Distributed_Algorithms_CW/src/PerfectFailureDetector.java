import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;


public class PerfectFailureDetector implements IFailureDetector {
	Process p;
	LinkedList<Integer> suspects;
	Timer t;
	
	private int[] numOfMsgsReceivedFromOthers_old;
	
	private int[] numOfMsgsReceivedFromOthers_new;
	
	static final String HeartbeatMessage="heartbeat";
	
	static final String NULL_MSG="null";
	
	static final int Delta=2000; /* 2second*/
	
	static final int Timeout = Delta+Utils.DELAY;
	
	
	class PeriodicTask extends TimerTask {
		private int numOfBroadcastsInTheChecking=-1;
		
		private PerfectFailureDetector pfd;
		private int len;
		
		public PeriodicTask(PerfectFailureDetector pfd){
			this.pfd=pfd;
			this.len=this.pfd.numOfMsgsReceivedFromOthers_old.length;
		}

		@Override
		public void run() {
			p.broadcast(HeartbeatMessage, String.format("%d", 
						System.currentTimeMillis()));
			
			numOfBroadcastsInTheChecking++;
			
			if(numOfBroadcastsInTheChecking==2){
				check();
				
				numOfBroadcastsInTheChecking=0;
			}
		}

		private void check() {
			
			for (int index = 1; index < len; index++) {
				if(index==this.pfd.p.pid)
					continue;
				
				//if we haven't heard from a process within two
				//broadcasts times, then it should be failed.
				// the total timeout is delta + delay. As these two are 
				//variables, we can make delta greater than delay, then 
				//delta+delay should be definitely less than twice delta.
				//therefore, if a process is not crashed, we should be able to 
				//get the message within two delta time.
				if(this.pfd.numOfMsgsReceivedFromOthers_old[index]
					==this.pfd.numOfMsgsReceivedFromOthers_new[index]){
					
					this.pfd.isSuspected(index);

				}
			}
			
			for (int index = 1; index < len; index++) {
			//update the numbers of msgs table.
			this.pfd.numOfMsgsReceivedFromOthers_old[index]=
					this.pfd.numOfMsgsReceivedFromOthers_new[index];
			}
			
		}
		
	}
	
	

	public PerfectFailureDetector(Process p) {
		super();
		this.p = p;
		this.t=new Timer();
		this.suspects=new LinkedList<Integer>();
		
		init();
	}
	
	

	private void init() {
		this.numOfMsgsReceivedFromOthers_old=new int[this.p.n+1];
		this.numOfMsgsReceivedFromOthers_new=new int[this.p.n+1];
	}



	@Override
	public void begin() {
		this.t.schedule(new PeriodicTask(this), 0, Delta);

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
		
		System.out.println("Process "+pid+
				" is suspected due to timeout");
		}
	}

	@Override
	public void receive(Message m) {
		
		long delay=	System.currentTimeMillis()-
					Long.parseLong(m.getPayload());
		
		System.out.println("The delay is "+delay+" miliseconds");
		
		int herPID=m.getSource();
		
		this.numOfMsgsReceivedFromOthers_new[herPID]++;
		
//		Utils.out(p.pid,m.toString());		

	}

}
