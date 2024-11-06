package tech.ammer.sdk.card.pax;


import static tech.ammer.sdk.card.pax.AmmerCardException.Type.COMMUNICATION_ERROR;

import com.pax.dal.exceptions.AGeneralException;

public abstract class ReaderWrapper {

    protected abstract byte[] cmdExchange(byte[] buffer) throws AGeneralException;

    public byte[] processCommand(byte[] buffer) throws AmmerCardException {
        try {
            return cmdExchange(buffer);
//            if (response != null &&  response.length >= 2) {
//                short sw = (short)Integer.parseInt(Hex.toHexString(Arrays.copyOfRange(response, response.length - 2, response.length)), 16);
//                switch (sw) {
//                    case ISO7816.SW_NO_ERROR:
//                        return Arrays.copyOfRange(response, 0, response.length - 2);
//                    case ISO7816.SW_CLA_NOT_SUPPORTED:
//                        throw new AmmerCardException(CLA_NOT_SUPPORTED);
//                    case ISO7816.SW_INS_NOT_SUPPORTED:
//                        throw new AmmerCardException(INSTRUCTION_NOT_SUPPORTED);
//                    case ISO7816.SW_CONDITIONS_NOT_SATISFIED:
//                        throw new AmmerCardException(INCORRECT_STATE);
//                    case ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED:
//                        throw new AmmerCardException(INCORRECT_PIN);
//                    case ISO7816.SW_WRONG_DATA:
//                        throw new AmmerCardException(INCORRECT_DATA);
//                    case ISO7816.SW_FILE_NOT_FOUND:
//                        throw new AmmerCardException(CARD_NOT_SUPPORTED);
//                    case EMV.SW_APPLICATION_IS_PERMANENTLY_LOCKED:
//                        throw new AmmerCardException(CARD_ERASED);
//                    default:
//                        throw new AmmerCardException(COMMUNICATION_ERROR);
//                }
//            }
//            throw new AmmerCardException(COMMUNICATION_ERROR);
        } catch (AGeneralException e) {
            throw new AmmerCardException(COMMUNICATION_ERROR, e);
        }
    }

}
