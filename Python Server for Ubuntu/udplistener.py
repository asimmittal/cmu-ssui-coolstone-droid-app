"""
	udplistener.py
	--------------
	This is a simple script that will listen on a specified UDP port and display 
	incoming datagram packets
"""

import time;
from socket import *

if __name__ == '__main__':

	#IP = '0.0.0.0'
	PORT = 52226
	sock = socket(AF_INET,SOCK_DGRAM)
	sock.bind(("localhost",PORT));
	
	print 'Listening on localhost on port', PORT
	
	while True:
		
		try:
			data, addr = sock.recvfrom(1024)
			print 'Received:' , data, "from", addr
		except KeyboardIntterupt:
			break;
			
	print '>>>>>>>>>>>>>> END OF SCRIPT <<<<<<<<<<<<<<<<<<'