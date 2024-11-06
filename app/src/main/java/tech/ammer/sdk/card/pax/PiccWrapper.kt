package tech.ammer.sdk.card.pax

import com.pax.dal.IPicc
import com.pax.dal.exceptions.AGeneralException

class PiccWrapper(private val picc: IPicc) : ReaderWrapper() {

    @Throws(AGeneralException::class)
    public override fun cmdExchange(buffer: ByteArray): ByteArray {
        return picc.cmdExchange(buffer, 256)
    }
}
