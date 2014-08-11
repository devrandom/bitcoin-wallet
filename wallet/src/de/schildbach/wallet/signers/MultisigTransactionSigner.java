package de.schildbach.wallet.signers;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.crypto.TransactionSignature;
import com.google.bitcoin.script.Script;
import com.google.bitcoin.wallet.KeyBag;

public class MultisigTransactionSigner extends AbstractTransactionSigner {
    private static final Logger log = LoggerFactory.getLogger(MultisigTransactionSigner.class);

	@Override
	public boolean signInputs(Transaction tx, KeyBag keyBag) {
        int numInputs = tx.getInputs().size();
        for (int i = 0; i < numInputs; i++) {
            TransactionInput txIn = tx.getInput(i);
            if (txIn.getConnectedOutput() == null) {
                log.warn("Missing connected output, assuming input {} is already signed.", i);
                continue;
            }
            
            boolean isP2SH = txIn.getConnectedOutput().getScriptPubKey().isPayToScriptHash();
            Script inputScript = txIn.getScriptSig();
            if (!isP2SH)
            	continue;
            Script script = getRedeemScript(inputScript);
            List<byte[]> pubkeys = script.getPubKeys();
            ECKey key = null;
            for (byte[] pubkey : pubkeys) {
            	key = keyBag.findKeyFromPubKey(pubkey);
            	// FIXME check if this key can sign
            	if (key != null && canSign(key))
            		break;
            	key = null;
            }
            if (key == null) {
            	log.warn("Could not find privkey");
            	continue;
            }
            
            TransactionSignature signature;
            try {
                signature = tx.calculateSignature(i, key, script, Transaction.SigHash.ALL, false);
                int index = getKeyPosition(txIn, key, inputScript);
                if (index < 0)
                    throw new RuntimeException("Input script doesn't contain our key"); // This should not happen
                inputScript = inputScript.addSignature(index, signature, isP2SH);
                txIn.setScriptSig(inputScript);
            } catch (ECKey.KeyIsEncryptedException e) {
                throw e;
            }
        }	
        
        return true;
	}

	private boolean canSign(ECKey key) {
		if (key instanceof DeterministicKey) {
			DeterministicKey cursor = (DeterministicKey)key;
			while (cursor != null) {
				if (cursor.hasPrivKey())
					return true;
				cursor = cursor.getParent();
			}
			return false;
		} else {
			return key.hasPrivKey();
		}
	}
}
