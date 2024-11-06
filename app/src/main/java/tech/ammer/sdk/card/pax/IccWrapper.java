package tech.ammer.sdk.card.pax;

import com.pax.dal.IIcc;
import com.pax.dal.exceptions.AGeneralException;

public class IccWrapper extends ReaderWrapper {

    private IIcc icc;

    public IccWrapper(IIcc icc) {
        this.icc = icc;
    }

    @Override
    public byte[] cmdExchange(byte[] buffer) throws AGeneralException {
        return icc.cmdExchange((byte) 128, buffer);
    }
}
