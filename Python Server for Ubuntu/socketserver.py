"""
	SocketServer.py
	---------------
	Socket server that receives accelerometer data (3-axes) from the client
	protocol for data <data>{value};{value};{value}</data>
	
	The server will spawn a client thread and invoke a callback function that 
	may be used to handle the data
	
"""

from socket import *
import threading

class SocketServer:
	
	def __init__(self,address,callback=None):
		"""
			address --> tuple containing ip,port e.g.('127.0.0.1',5555)
			callback --> function name that needs to be called to handle accel data
		"""
		
		self.ip = address[0]					#save the ip address
		self.port = address[1]					#save the port number
		self.dataHandler = callback;				#save the callback
		self.isRunning = False;					#flag to control liftime of the server
		
		if(isinstance(self.port,int) == False): self.port = 62222
		
	def __processDataBuffer(self,dataBuffer):
		"""
			This is where the packet filteration routine can be written
			this routine returns the string representing the packet
			from the buffer
		"""
		
		#convert all characters to lower case
		dataBuffer = dataBuffer.lower();
		
		#delimiters for the packet
		toReturn = None;
		startTag = '<data>'
		endTag = '</data>'
		
		#if the delimiters are found in the string, get the data in betwee
		if((startTag in dataBuffer) and (endTag in dataBuffer)):
			startIndex = dataBuffer.find(startTag); 
			endIndex = dataBuffer.find(endTag); 
			return dataBuffer[startIndex:endIndex].replace(startTag,'')
			
		
		#return the data
		return toReturn;

	def startServerThread(self):
		thread = threading.Thread(target=self.__startServer)
		thread.start();

	def stopServerThread(self):
		self.isRunning = false
		
	def __startServer(self):
		"""
			This is the code for the server. the server services only a single client at a given time.
			the client sends packets, which are filtered and a callback function (if one is assigned)
			is invoked to handle the packet data.
		"""
		
		#initialize the socket for the server and bind it
		servSocket = socket(AF_INET,SOCK_STREAM)									
		servSocket.bind((self.ip,self.port))
		servSocket.listen(5)
		
		#enable the server loop to run
		self.isRunning = True
		
		#the loop is a basic server, where a connection is accepted and serviced
		while self.isRunning:
			
			print ('TCP Server is running on:',self.ip,self.port)
			
			#wait for a connection. this call is blocking
			clientSock,address = servSocket.accept()
			dataBuffer = ''
			
			print ("Connection received")
			
			#if the connection comes, the control will come here
			while True:
			
				#receive a byte and add it to the data buffer
				try:
					byte = clientSock.recv(1)
					if byte == '': 
						raise ConnClosedException;
					dataBuffer += byte;
				except:
					print("connection closed!")
					isRunning = False;
					break
				
				#process this buffer
				packet = self.__processDataBuffer(dataBuffer);
				
				#act upon this packet after processing
				if(packet is not None):
					if self.dataHandler is None:
						print 'Received packet with data:',packet
					else:
						self.dataHandler(packet)
						
					dataBuffer = ''
				
				#check if the user still wants this server to run
				if(self.isRunning is False):
					clientSock.close();
					break;

		print "Closing Server Thread"
		isRunning = false;
					
			
			
			
