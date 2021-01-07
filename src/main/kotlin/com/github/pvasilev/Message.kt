package com.github.pvasilev

import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.nio.ByteBuffer

sealed class Message(val header: Header)

class Question(
	header: Header,
	val qname: String,
	val qtype: Qtype,
	val qclass: Qclass = Qclass.INET,
) : Message(header) {

	enum class Qtype {
		A,
		AAAA,
		CNAME
	}

	enum class Qclass {
		INET
	}

	fun serialize(): ByteBuffer {
		val parts = qname.split(".")
		val buffer = ByteBuffer.allocate(12 + parts.sumBy { it.length } + parts.size + 1 + 2 + 2)
		buffer.put(header.serialize())
		parts.forEach { part ->
			buffer.put(part.length.toByte())
			buffer.put(part.toByteArray())
		}
		buffer.put(0)
		buffer.putShort(
			when (qtype) {
				Qtype.A -> 1
				Qtype.AAAA -> 28
				Qtype.CNAME -> 5
			}
		)
		buffer.putShort(
			when (qclass) {
				Qclass.INET -> 1
			}
		)
		buffer.position(0)
		return buffer
	}
}

class Answer(
	header: Header,
	val hostname: String,
	val addresses: List<InetAddress>,
) : Message(header) {

	companion object {
		@ExperimentalUnsignedTypes
		fun deserialize(bytes: ByteArray): Answer {
			val buffer = ByteBuffer.wrap(bytes)
			val header = Header.deserialize(bytes)
			buffer.position(12)
			val hostname = mutableListOf<String>()
				.apply {
					var size = buffer.get()
					while (size.toInt() != 0) {
						val dst = ByteArray(size.toInt())
						buffer.get(dst)
						this += String(dst)
						size = buffer.get()
					}
				}
				.joinToString(separator = ".")
			buffer.position(buffer.position() + 4)
			val addresses = mutableListOf<InetAddress>()
			for (i in 0 until header.anCount) {
				val skip = 2 + 2 + 2 + 4
				buffer.position(buffer.position() + skip)
				val rdLength = buffer.short.toInt()
				val addr = ByteArray(rdLength)
				buffer.get(addr)
				addresses += if (rdLength == 4) {
					Inet4Address.getByAddress(addr)
				} else {
					Inet6Address.getByAddress(addr)
				}
			}
			return Answer(header, hostname, addresses)
		}
	}
}

class Header(
	val id: Short,
	val qr: Qr = Qr.QUERY,
	val opcode: Opcode = Opcode.QUERY,
	val aa: Boolean = false,
	val tc: Boolean = false,
	val rd: Boolean = false,
	val ra: Boolean = false,
	val rcode: Rcode = Rcode.NO_ERROR,
	val qdCount: Short = 1,
	val anCount: Short = 0,
	val nsCount: Short = 0,
	val arCount: Short = 0
) {

	enum class Qr {
		QUERY,
		RESPONSE
	}

	enum class Opcode {
		QUERY,
		IQUERY,
		STATUS
	}

	enum class Rcode {
		NO_ERROR,
		FORMAT_ERROR,
		SERVER_FAILURE,
		NAME_ERROR,
		NOT_IMPLEMENTED,
		REFUSED
	}

	companion object {
		@ExperimentalUnsignedTypes
		fun deserialize(bytes: ByteArray): Header {
			val buffer = ByteBuffer.wrap(bytes)
			val id = buffer.short
			val flags = buffer.short
			val qdCount = buffer.short
			val anCount = buffer.short
			val nsCount = buffer.short
			val arCount = buffer.short
			return Header(
				id = id,
				qr = if (flags.toUInt() shr 15 and 1u == 1u) Qr.RESPONSE else Qr.QUERY,
				opcode = when (flags.toUInt() shr 11 and 15u) {
					0u -> Opcode.QUERY
					1u -> Opcode.IQUERY
					2u -> Opcode.STATUS
					else -> throw IllegalStateException()
				},
				aa = flags.toUInt() shr 10 and 1u == 1u,
				tc = flags.toUInt() shr 9 and 1u == 1u,
				rd = flags.toUInt() shr 8 and 1u == 1u,
				ra = flags.toUInt() shr 7 and 1u == 1u,
				rcode = when (flags.toUInt() and 15u) {
					0u -> Rcode.NO_ERROR
					1u -> Rcode.FORMAT_ERROR
					2u -> Rcode.SERVER_FAILURE
					3u -> Rcode.NAME_ERROR
					4u -> Rcode.NOT_IMPLEMENTED
					5u -> Rcode.REFUSED
					else -> throw IllegalStateException()
				},
				qdCount = qdCount,
				anCount = anCount,
				nsCount = nsCount,
				arCount = arCount
			)
		}
	}

	fun serialize(): ByteBuffer {
		val buffer = ByteBuffer.allocate(12)
		buffer.putShort(id)
		val flags = buildString {
			when (qr) {
				Qr.QUERY -> append("0")
				Qr.RESPONSE -> append("1")
			}
			when (opcode) {
				Opcode.QUERY -> append("0000")
				Opcode.IQUERY -> append("0001")
				Opcode.STATUS -> append("0010")
			}
			if (aa) append("1") else append("0")
			if (tc) append("1") else append("0")
			if (rd) append("1") else append("0")
			if (ra) append("1") else append("0")
			append("000")
			when (rcode) {
				Rcode.NO_ERROR -> append("0000")
				Rcode.FORMAT_ERROR -> append("0001")
				Rcode.SERVER_FAILURE -> append("0010")
				Rcode.NAME_ERROR -> append("0011")
				Rcode.NOT_IMPLEMENTED -> append("0100")
				Rcode.REFUSED -> append("0101")
			}
		}
		buffer.putShort(flags.toShort(radix = 2))
		buffer.putShort(qdCount)
		buffer.putShort(anCount)
		buffer.putShort(nsCount)
		buffer.putShort(arCount)
		buffer.position(0)
		return buffer
	}
}