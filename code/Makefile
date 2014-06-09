#
# Define the variable for the Java Compiler
#
JCC = javac

#
# Define the variable for the JavaCompiler Flags
#
#JFLAGS = -Xlint:deprecation
JFLAGS = 

#
# tell make about java source and binary files
#
.SUFFIXES: .java .class

#
# tell make what to do with java files
#
.java.class:
	$(JCC) $(JFLAGS) $<

#
# Define the default target to compile both Server and Client
#
default: Server Client

#
# Define the target for building the server application
#
Server: server.class PacketHelper Packet PacketReceiver PacketSender SimpleFileWriter

#
# Define the target for building the client application
#
Client: client.class PacketHelper Packet FileChunker GoBackNProtocol PacketReceiver PacketSender

FileChunker: FileChunker.class
SimpleFileWriter: SimpleFileWriter.class
GoBackNProtocol: GoBackNProtocol.class
PacketReceiver: PacketReceiver.class
PacketSender: PacketSender.class
PacketHelper: PacketHelper.class
Packet: packet.class

#
# Clean Target
#
clean:
	$(RM) server.class
	$(RM) client.class
	$(RM) FileChunker.class
	$(RM) SimpleFileWriter.class
	$(RM) GoBackNProtocol.class
	$(RM) PacketReceiver.class
	$(RM) PacketReceiver*INotifyPacketArrived.class
	$(RM) PacketSender.class
	$(RM) PacketHelper.class
	$(RM) PacketHelper*PacketType.class
	$(RM) packet.class
