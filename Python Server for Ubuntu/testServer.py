from netsettings import *;

dctCommands = {

	"zoom" : "xte 'keydown Control_L' 'keydown Alt_L' 'str =' 'keyup Control_L' 'keyup Alt_L'",
	"pinch": "xte 'keydown Control_L' 'keydown Alt_L' 'str -' 'keyup Control_L' 'keyup Alt_L'",
	"swipe_left": "xte 'keydown Control_L' 'keydown Alt_L' 'key Left' 'keyup Control_L' 'keyup Alt_L'",
	"swipe_right": "xte 'keydown Control_L' 'keydown Alt_L' 'key Right' 'keyup Control_L' 'keyup Alt_L'",
	"grab_3": "xte 'keydown Control_L' 'keydown Alt_L' 'str 9' 'keyup Control_L' 'keyup Alt_L'",
	"grab_4": "xte 'keydown Control_L' 'keydown Alt_L' 'str 0' 'keyup Control_L' 'keyup Alt_L'",
	"grab_off": "xte 'key Escape'",
	"face_down": "xte 'keydown Control_L' 'keydown Alt_L' 'str l' 'keyup Control_L' 'keyup Alt_L'",
}

def tcpMessageReceivedHandler(message):
	import os
	try:
		strCommand = dctCommands[message]
		os.system(strCommand)
		print "Command executed: " + message
	except:
		print "Command not found --> " + message
	

if __name__ == '__main__':

	tcpPort = 52225
	udpPort = 52226
	
	broadcastAddr, ipAddr = getBroadcastAddress();
	devName = getDeviceName();
	msgBcast = ipAddr+';'+str(tcpPort)+';'+devName
	
	server = SocketServer((ipAddr,tcpPort),tcpMessageReceivedHandler);
	broadcaster = UDPBroadcaster(msgBcast,udpPort,broadcastAddr,1,True);
	
	server.startServerThread();
	broadcaster.startBroadcasting();
