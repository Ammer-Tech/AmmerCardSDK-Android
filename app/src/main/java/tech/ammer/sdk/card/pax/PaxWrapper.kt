package tech.ammer.sdk.card.pax

import android.content.Context
import android.util.Log
import com.pax.dal.ICardReaderHelper
import com.pax.dal.IDAL
import com.pax.dal.IIcc
import com.pax.dal.IPicc
import com.pax.dal.IPrinter
import com.pax.dal.entity.EPiccType
import com.pax.dal.entity.EReaderType
import com.pax.dal.entity.PollingResult
import com.pax.dal.exceptions.IccDevException
import com.pax.dal.exceptions.MagDevException
import com.pax.dal.exceptions.PiccDevException
import com.pax.neptunelite.api.NeptuneLiteUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.CoroutineContext
import kotlin.properties.Delegates

object PaxWrapper {

    private var paxInterface: PaxInterface by Delegates.notNull()

    private var paxEventListener: PaxEventListener? = null

    private var workerContact: WorkerContact? = null
    private var workerNFC: WorkerNFC? = null
    private var paxInterfaceRef: AtomicReference<PaxInterface?> by Delegates.notNull()
    private var dal: IDAL by Delegates.notNull()
    private var icc: IIcc by Delegates.notNull()
    private var picc: IPicc by Delegates.notNull()
    private var piccHelper: ICardReaderHelper by Delegates.notNull()

    var printer: IPrinter? = null
        private set
    var paxEnable = false
        private set

    fun start(context: Context, paxInterface: PaxInterface) {
        try {
            this.paxEventListener = paxEventListener
            this.paxInterface = paxInterface
            paxInterfaceRef = AtomicReference(null)
            dal = NeptuneLiteUser.getInstance().getDal(context)
            icc = dal.icc
            picc = dal.getPicc(EPiccType.INTERNAL)
            piccHelper = dal.cardReaderHelper
            printer = dal.printer
            paxEnable = true
        } catch (e: Throwable) {
            paxEnable = false
            e.printStackTrace()
        }
    }


    fun setEventListener(paxEventListener: PaxEventListener) {
        this.paxEventListener = paxEventListener
    }

    fun enable() {
        if (workerContact != null && workerContact!!.isActive || workerNFC != null && workerNFC!!.isActive) {
            Log.d("PaxWrapper", "PaxWrapper already running")
            workerContact?.resume()
            workerNFC?.resume()
            return
        }
        when (paxInterface) {
            PaxInterface.PAX_INTERFACE_ALL -> {
                workerContact = WorkerContact()
                workerNFC = WorkerNFC()
            }

            PaxInterface.PAX_INTERFACE_CONTACT -> workerContact = WorkerContact()
            PaxInterface.PAX_INTERFACE_NFC -> workerNFC = WorkerNFC()
            else -> {}
        }
        workerContact?.start()
        workerNFC?.start()
    }

    fun stop() {
//        workerContact?.terminate()
//        workerNFC?.terminate()
    }

    fun destroy() {
        workerContact?.coroutineContext?.cancel()
        workerNFC?.coroutineContext?.cancel()

        workerContact = null
        workerNFC = null
    }

    fun pause() {
        workerNFC?.terminate()
        workerContact?.terminate()

        paxInterfaceRef.set(null)
    }

    internal class WorkerContact : CoroutineScope {
        override val coroutineContext: CoroutineContext = SupervisorJob() + Dispatchers.IO

        private val ISO_7816 = 128.toByte()
        private val terminated = AtomicBoolean(false)

        fun start() {
            launch {
                try {
                    Log.d("PaxWrapper", "WorkerContact started")
                    while (true) {
                        if (!terminated.get())
                            if (paxInterfaceRef.get() == null || paxInterfaceRef.get() == PaxInterface.PAX_INTERFACE_CONTACT) {
                                try {
                                    val cardInserted = icc.detect(ISO_7816)
                                    if (paxInterfaceRef.get() == null && cardInserted) {
                                        icc.init(128.toByte())
                                        paxInterfaceRef.set(PaxInterface.PAX_INTERFACE_CONTACT)
                                        paxEventListener?.cardAttached(IccWrapper(icc), PaxInterface.PAX_INTERFACE_CONTACT)
                                    } else if (paxInterfaceRef.get() == PaxInterface.PAX_INTERFACE_CONTACT && !cardInserted) {
                                        paxInterfaceRef.set(null)
                                        paxEventListener?.cardDetached(PaxInterface.PAX_INTERFACE_CONTACT)
                                        icc.close(128.toByte())
                                    }
                                } catch (e: IccDevException) {
                                    // Do nothing
                                }
                            }
                        delay(100)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                Log.d("PaxWrapper", "WorkerContact stopped")
            }
        }

        fun terminate() {
            terminated.set(true)
        }

        fun resume() {
            terminated.set(false)
        }
    }

    private class WorkerNFC : CoroutineScope {
        override val coroutineContext: CoroutineContext = SupervisorJob() + Dispatchers.IO
        private val terminated = AtomicBoolean(false)
        fun start() {
            launch {
                try {
                    Log.d("PaxWrapper", "WorkerNFC started")
                    while (true) {
                        if (!terminated.get())
                            if (paxInterfaceRef.get() == null || paxInterfaceRef.get() == PaxInterface.PAX_INTERFACE_NFC) {
                                try {
                                    val result = piccHelper.polling(EReaderType.PICC, 100)
                                    if (paxInterfaceRef.get() == null && result.cardType == 'A'.code.toByte() && result.operationType == PollingResult.EOperationType.OK) {
                                        paxInterfaceRef.set(PaxInterface.PAX_INTERFACE_NFC)
                                        paxEventListener?.cardAttached(PiccWrapper(picc), PaxInterface.PAX_INTERFACE_NFC)
                                    } else if (paxInterfaceRef.get() == PaxInterface.PAX_INTERFACE_NFC && result.cardType == 0.toByte()) {
                                        paxInterfaceRef.set(null)
                                        paxEventListener?.cardDetached(PaxInterface.PAX_INTERFACE_NFC)
                                    }
                                } catch (_: MagDevException) {
                                } catch (_: IccDevException) {
                                } catch (_: PiccDevException) {
                                }
                            }
                        //Prevent high CPU load
                        delay(100)
                    }
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
                Log.d("PaxWrapper", "WorkerNFC stopped")
            }
        }

        fun terminate() {
            terminated.set(true)
        }

        fun resume() {
            terminated.set(false)
        }
    }

//    companion object {
//        private var paxWrapper: PaxWrapper? = null
//
//        @Throws(Exception::class)
//        fun init(context: Context, paxInterface: PaxInterface): PaxWrapper? {
//            if (paxWrapper == null) {
//                paxWrapper = PaxWrapper(context, paxInterface)
//            }
//            return paxWrapper
//        }
//
//        @get:Throws(Exception::class)
//        val instance: PaxWrapper?
//            get() {
//                checkNotNull(paxWrapper)
//                return paxWrapper
//            }
//    }
}