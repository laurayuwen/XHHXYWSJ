
class ELEProcess extends Process
{
	private IFailureDetector detector;
	
	public ELEProcess(String name, int pid, int workload) {
		super(name, pid, workload);
		this.detector=new EventuallyLeaderElector(this);
	}
	
	public void begin(){
		this.detector.begin();
	}

	public synchronized void receive(Message m){
		String type=m.getType();
		if(type!=null && type.equals(EventuallyLeaderElector.HeartbeatMessage)){
			this.detector.receive(m);
		}
	}
	
	public static void main(String[] args) 
	{		
		String pName=args[0];
		int id=Integer.parseInt(args[1]);
		int n=Integer.parseInt(args[2]);
		ELEProcess p=new ELEProcess(pName,id,n);
		p.registeR();
		p.begin();
	}
}
