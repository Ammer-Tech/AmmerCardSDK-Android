//package tech.ammer.pax
//
//import ammer.pay.apdu.APDUBuilder
//import ammer.pay.apdu.ISO7816
//import ammer.pay.apdu.Instructions
//import ammer.pay.apdu.TLV
//import ammer.pay.apdu.Tags
//import android.util.Log
//import org.bouncycastle.util.encoders.Hex
//import tech.ammer.pax.tlv.SimpleTLV
//import java.io.ByteArrayOutputStream
//import java.nio.ByteBuffer
//import java.util.Arrays
//import java.util.UUID
//
//class AmmerCard(private val readerWrapper: ReaderWrapper) {
//    internal enum class AID {
//        A0_00_00_08_82_00_02,
//        A0_00_00_08_82_00_01
//    }
//
//    @Throws(AmmerCardException::class)
//    fun select(): UUID {
//        var lastException: AmmerCardException? = null
//        for (s in AID.entries) {
//            try {
//                val cmd = APDUBuilder
//                    .init()
//                    .setINS(ISO7816.INS_SELECT)
//                    .setP1(0x04.toByte())
//                    .setP2(0x00.toByte())
//                    .setData(Hex.decode(s.toString().replace("_".toRegex(), "")))
//                    .build()
//
//                readerWrapper.processCommand(cmd)
//
//                return cardGUID
//            } catch (e: AmmerCardException) {
//                lastException = e
//            }
//        }
//        throw lastException!!
//    }
//
//    @get:Throws(AmmerCardException::class)
//    val cardGUID: UUID
//        get() {
//            val response =
//                readerWrapper.processCommand(APDUBuilder.init().setINS(Instructions.INS_GET_CARD_GUID).build())
//            val guidFromCard =
//                Arrays.copyOfRange(response, TLV.OFFSET_VALUE.toInt(), TLV.HEADER_BYTES_COUNT + Tags.CARD_GUID_LENGTH)
//
//            val buffer = ByteBuffer.allocate(8)
//            buffer.put(guidFromCard, 0, 8)
//            buffer.flip() //need flip
//            val mostSigBits = buffer.getLong()
//            buffer.clear()
//            buffer.put(guidFromCard, 8, 8)
//            buffer.flip()
//            val leastSigBits = buffer.getLong()
//
//            return UUID(mostSigBits, leastSigBits)
//        }
//
//    @get:Throws(AmmerCardException::class)
//    val cardIssuer: Int
//        get() = (ByteBuffer.wrap(
//            readerWrapper.processCommand(
//                APDUBuilder.init().setINS(Instructions.INS_GET_CARD_ISSUER).build()
//            )
//        ).getShort(TLV.OFFSET_VALUE.toInt()).toInt() and 0xFFFF)
//
//    @get:Throws(AmmerCardException::class)
//    val cardSeries: Int
//        get() = (ByteBuffer.wrap(
//            readerWrapper.processCommand(
//                APDUBuilder.init().setINS(Instructions.INS_GET_CARD_SERIES).build()
//            )
//        ).getShort(TLV.OFFSET_VALUE.toInt()).toInt() and 0xFFFF)
//
//    @get:Throws(AmmerCardException::class)
//    val pINRetries: Int
//        get() = (readerWrapper.processCommand(
//            APDUBuilder.init().setINS(Instructions.INS_GET_PIN_RETRIES).build()
//        )[TLV.OFFSET_VALUE.toInt()].toInt() and 0xFF)
//
//    @get:Throws(AmmerCardException::class)
//    val processingPublicKey: ByteArray
//        get() {
//            val response =
//                readerWrapper.processCommand(APDUBuilder.init().setINS(Instructions.INS_GET_PROCESSING_PUBLIC_KEY).build())
//            return Arrays.copyOfRange(
//                response,
//                TLV.OFFSET_VALUE.toInt(),
//                TLV.HEADER_BYTES_COUNT + Tags.PROCESSING_PUBLIC_KEY_LENGTH
//            )
//        }
//
//    @get:Throws(AmmerCardException::class)
//    val public: ByteArray
//        get() {
//            val response =
//                readerWrapper.processCommand(APDUBuilder.init().setINS(Instructions.INS_GET_PUBLIC_KEY).build())
//            return Arrays.copyOfRange(
//                response,
//                TLV.OFFSET_VALUE.toInt(),
//                TLV.HEADER_BYTES_COUNT + Tags.CARD_PUBLIC_KEY_LENGTH
//            )
//        }
//
//    @get:Throws(AmmerCardException::class)
//    val state: Int
//        get() = (readerWrapper.processCommand(
//            APDUBuilder.init().setINS(Instructions.INS_GET_STATE).build()
//        )[TLV.OFFSET_VALUE.toInt()].toInt() and 0xFF)
//
//    @Throws(AmmerCardException::class)
//    fun lock(): Int {
//        readerWrapper.processCommand(APDUBuilder.init().setINS(Instructions.INS_LOCK).build())
//        return state
//    }
//
//    @Throws(AmmerCardException::class)
//    fun signData(pin: String, dataForSign: ByteArray): ByteArray {
//        val pinTLV = pinToTLV(pin)
//        val dataForSignTLV = SimpleTLV.init(Tags.DATA_FOR_SIGN, dataForSign.size).set(dataForSign).build()
//        val out = ByteArrayOutputStream()
//        out.write(pinTLV, 0, pinTLV.size)
//        out.write(dataForSignTLV, 0, dataForSignTLV.size)
//
//        val response = readerWrapper.processCommand(APDUBuilder.init().setINS(Instructions.INS_SIGN_DATA).setData(out.toByteArray()).build())
//        return Arrays.copyOfRange(response, TLV.OFFSET_VALUE.toInt(), TLV.HEADER_BYTES_COUNT + response[TLV.OFFSET_LENGTH.toInt()])
//    }
//
//    @Throws(AmmerCardException::class)
//    fun signProcessingData(dataForSign: ByteArray, gatewaySignature: ByteArray): ByteArray {
//        val dataForSignTLV = SimpleTLV.init(Tags.DATA_FOR_SIGN, dataForSign.size).set(dataForSign).build()
//        val gatewayTLV = SimpleTLV.init(Tags.DATA_SIGNATURE, gatewaySignature.size).set(gatewaySignature).build()
//
//        val out = ByteArrayOutputStream()
//        out.write(dataForSignTLV, 0, dataForSignTLV.size)
//        out.write(gatewayTLV, 0, gatewayTLV.size)
//        val request = APDUBuilder.init().setINS(Instructions.INS_SIGN_PROCESSING_DATA).setData(out.toByteArray()).build()
//        Log.d("#!Processing:", "" + request.contentToString())
//        val response = readerWrapper.processCommand(request)
//        return Arrays.copyOfRange(response, TLV.OFFSET_VALUE.toInt(), TLV.HEADER_BYTES_COUNT + response[TLV.OFFSET_LENGTH.toInt()])
//    }
//
//    @Throws(AmmerCardException::class)
//    fun unlock(pin: String): Int {
//        readerWrapper.processCommand(APDUBuilder.init().setINS(Instructions.INS_UNLOCK).setData(pinToTLV(pin)).build())
//        return state
//    }
//
//    private fun pinToTLV(pin: String): ByteArray {
//        val pinBytes = pin.toByteArray()
//        for (i in pinBytes.indices) {
//            pinBytes[i] = (pinBytes[i] - 0x30).toByte()
//        }
//        return SimpleTLV.init(Tags.CARD_PIN, pinBytes.size).set(pinBytes).build()
//    }
//}
