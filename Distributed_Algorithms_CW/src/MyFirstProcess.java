class MyFirstProcess extends Process
{
	private IFailureDetector detector;
	
	public MyFirstProcess(String name, int pid, int workload) {
		super(name, pid, workload);
		this.detector=new MyFirstFailureDetector(this);
	}
	
	public void begin(){
		this.detector.begin();
	}

	public synchronized void receive(Message m){
		String type=m.getType();
		if(type!=null && type.equals(MyFirstFailureDetector.HeartbeatMessage)){
			this.detector.receive(m);
		}
	}
	
	public static void main(String[] args) 
	{		
		String pName=args[0];
		int id=Integer.parseInt(args[1]);
		int n=Integer.parseInt(args[2]);
		MyFirstProcess p=new MyFirstProcess(pName,id,n);
		p.registeR();
		p.begin();
	}
}
