package com.github.pvasilev

import java.io.DataOutputStream
import java.net.*

val dns: DnsClient = DnsClient.DEFAULT

interface DnsClientDelegate {

	val server: InetAddress

	fun query(question: Question): Answer
}

class DnsOverUdp(override val server: InetAddress) : DnsClientDelegate {

	private companion object {
		const val BUFFER_SIZE = 1024
	}

	@ExperimentalUnsignedTypes
	override fun query(question: Question): Answer {
		val outputBuffer = question.serialize().array()
		val inputBuffer = ByteArray(BUFFER_SIZE)

		val socket = DatagramSocket()
		val address = InetSocketAddress(server, DnsClient.PORT)

		val request = DatagramPacket(outputBuffer, outputBuffer.size, address)
		socket.send(request)

		val response = DatagramPacket(inputBuffer, inputBuffer.size)
		socket.receive(response)

		socket.close()

		return Answer.deserialize(inputBuffer)
	}
}

class DnsOverTcp(override val server: InetAddress) : DnsClientDelegate {

	private companion object {
		const val PREFIX_SIZE = 2L
	}

	@ExperimentalUnsignedTypes
	override fun query(question: Question): Answer {
		val socket = Socket(server, DnsClient.PORT)
		val outputStream = DataOutputStream(socket.getOutputStream())
		val inputStream = socket.getInputStream()

		val outputBuffer = question.serialize().array()
		outputStream.writeShort(outputBuffer.size)
		outputStream.write(outputBuffer)

		inputStream.skip(PREFIX_SIZE)
		val inputBuffer = inputStream.readBytes()

		socket.close()

		return Answer.deserialize(inputBuffer)
	}
}

class DnsClient(private val delegate: DnsClientDelegate) {

	companion object {
		private val GOOGLE_DNS = Inet4Address.getByAddress(byteArrayOf(8, 8, 8, 8))
		const val PORT = 53
		val DEFAULT = DnsClient(DnsOverUdp(GOOGLE_DNS))
	}

	fun lookup(hostname: String): List<String> {
		val header = Header(id = 1)
		val question = Question(header, hostname, Question.Qtype.A)
		val answer = delegate.query(question)
		return answer.addresses.map(InetAddress::toString)
	}
}