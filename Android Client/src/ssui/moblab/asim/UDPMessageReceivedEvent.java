package ssui.moblab.asim;

//this interface is used to raise events when UDP Packets are received.
public interface UDPMessageReceivedEvent {
	public void messageReceived(String message);
}
