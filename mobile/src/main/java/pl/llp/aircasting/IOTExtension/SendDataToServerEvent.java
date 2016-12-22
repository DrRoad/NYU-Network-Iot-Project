package pl.llp.aircasting.IOTExtension;
public class SendDataToServerEvent  {
	String msg;
	public SendDataToServerEvent(String msg){
		this.msg = msg;
	}
	public String getMsg(){
		return msg;
	}
	
}
