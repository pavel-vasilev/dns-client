package com.github.pvasilev

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

class Answer(header: Header) : Message(header)

class Header(
	val id: Short,
	val qr: Qr,
	val opcode: Opcode,
	val aa: Boolean,
	val tc: Boolean,
	val rd: Boolean,
	val ra: Boolean,
	val rcode: Rcode,
	val qdCount: Short,
	val anCount: Short,
	val nsCount: Short,
	val arCount: Short
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
		SERVER_FAILURE,
		NAME_ERROR,
		NOT_IMPLEMENTED,
		REFUSED
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
				Rcode.SERVER_FAILURE -> append("0001")
				Rcode.NAME_ERROR -> append("0010")
				Rcode.NOT_IMPLEMENTED -> append("0011")
				Rcode.REFUSED -> append("0100")
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