package de.schildbach.wallet.signers;

import java.util.Arrays;
import java.util.List;

import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.script.Script;
import com.google.bitcoin.script.ScriptChunk;
import com.google.bitcoin.signers.TransactionSigner;

public abstract class AbstractTransactionSigner implements TransactionSigner {
    protected int getKeyPosition(TransactionInput txIn, ECKey key, Script inputScript) {
        List<byte[]> pubKeys;
        boolean isP2SH = txIn.getConnectedOutput().getScriptPubKey().isPayToScriptHash();
        if (isP2SH) {
            Script redeemScript = getRedeemScript(inputScript);
            pubKeys = redeemScript.getPubKeys();
            for (int i = 0; i < pubKeys.size(); i++) {
                byte[] pubKey = pubKeys.get(i);
                if (Arrays.equals(pubKey, key.getPubKey()))
                    return i;
            }
            return -1;
        } else {
            return 0;
        }
    }

    protected Script getRedeemScript(Script inputScript) {
        List<ScriptChunk> chunks = inputScript.getChunks();
        byte[] program = chunks.get(chunks.size() - 1).data;
        return new Script(program);
    }
    
	@Override
	public boolean isReady() {
		return true;
	}

	@Override
	public byte[] serialize() {
		return new byte[0];
	}

	@Override
	public void deserialize(byte[] data) {
	}
}
