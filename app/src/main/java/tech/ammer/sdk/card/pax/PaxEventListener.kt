package tech.ammer.sdk.card.pax

interface PaxEventListener {
    fun cardAttached(iccWrapper: ReaderWrapper, paxInterfaceContact: PaxInterface)

    fun cardDetached(paxInterfaceContact: PaxInterface?)
}
