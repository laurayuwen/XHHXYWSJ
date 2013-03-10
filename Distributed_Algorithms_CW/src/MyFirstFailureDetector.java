import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;


public class MyFirstFailureDetector implements IFailureDetector {
	Process p;
	LinkedList<Integer> suspects;
	Timer t;
	
	static final String HeartbeatMessage="heartbeat";
	
	static final String NULL_MSG="null";
	
	static final int Delta=1000; /* 1second*/
	
	class PeriodicTask extends TimerTask {

		@Override
		public void run() {
			p.broadcast(HeartbeatMessage, NULL_MSG);
			
		}
		
	}
	
	

	public MyFirstFailureDetector(Process p) {
		super();
		this.p = p;
		this.t=new Timer();
		this.suspects=new LinkedList<Integer>();
	}

	@Override
	public void begin() {
		this.t.schedule(new PeriodicTask(), 0, Delta);

	}

	@Override
	public int getLeader() {
		// TODO Auto-generated method stub
		return -1;
	}

	@Override
	public boolean isSuspect(Integer pid) {
		// TODO Auto-generated method stub
		return this.suspects.contains(pid);
	}

	@Override
	public void isSuspected(Integer arg0) {
		

	}

	@Override
	public void receive(Message m) {
		Utils.out(p.pid,m.toString());

	}

}
