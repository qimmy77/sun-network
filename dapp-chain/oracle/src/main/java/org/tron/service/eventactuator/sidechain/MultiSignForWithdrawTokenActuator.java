package org.tron.service.eventactuator.sidechain;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Objects;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.tron.client.MainChainGatewayApi;
import org.tron.common.exception.RpcConnectException;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.WalletUtil;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Sidechain.EventMsg;
import org.tron.protos.Sidechain.EventMsg.EventType;
import org.tron.protos.Sidechain.MultiSignForWithdrawTokenEvent;
import org.tron.protos.Sidechain.TaskEnum;
import org.tron.service.check.TransactionExtensionCapsule;
import org.tron.service.eventactuator.Actuator;

@Slf4j(topic = "sideChainTask")
public class MultiSignForWithdrawTokenActuator extends Actuator {

  // "event MultiSignForWithdrawToken(address from, address mainChainAddress, uint256 valueOrTokenId, uint256 _type, bytes32 userSign, bytes32 dataHash, bytes32 txId);"
  private MultiSignForWithdrawTokenEvent event;
  @Getter
  private EventType type = EventType.MULTISIGN_FOR_WITHDRAW_TOKEN_EVENT;

  public MultiSignForWithdrawTokenActuator(String from, String mainChainAddress,
      String valueOrTokenId, String type, String userSign, String dataHash,
      String originalTransactionId, String transactionId) {
    ByteString fromBS = ByteString.copyFrom(WalletUtil.decodeFromBase58Check(from));
    ByteString mainChainAddressBS = ByteString
        .copyFrom(WalletUtil.decodeFromBase58Check(mainChainAddress));
    ByteString valueOrTokenIdBS = ByteString.copyFrom(ByteArray.fromString(valueOrTokenId));
    ByteString typeBS = ByteString.copyFrom(ByteArray.fromString(type));
    ByteString userSignBS = ByteString.copyFrom(ByteArray.fromHexString(userSign));
    ByteString dataHashBS = ByteString.copyFrom(ByteArray.fromHexString(dataHash));
    ByteString originalTransactionIdBS = ByteString
        .copyFrom(ByteArray.fromHexString(originalTransactionId));
    ByteString transactionIdBS = ByteString.copyFrom(ByteArray.fromHexString(transactionId));
    this.event = MultiSignForWithdrawTokenEvent.newBuilder().setFrom(fromBS)
        .setValueOrTokenId(valueOrTokenIdBS).setType(typeBS).setMainchainAddress(mainChainAddressBS)
        .setUserSign(userSignBS).setDataHash(dataHashBS)
        .setOriginalTransactionId(originalTransactionIdBS).setTransactionId(transactionIdBS)
        .build();
  }

  public MultiSignForWithdrawTokenActuator(EventMsg eventMsg)
      throws InvalidProtocolBufferException {
    this.event = eventMsg.getParameter().unpack(MultiSignForWithdrawTokenEvent.class);
  }

  @Override
  public TransactionExtensionCapsule createTransactionExtensionCapsule()
      throws RpcConnectException {
    if (Objects.nonNull(transactionExtensionCapsule)) {
      return this.transactionExtensionCapsule;
    }

    String fromStr = WalletUtil.encode58Check(event.getFrom().toByteArray());
    String valueOrTokenIdStr = event.getValueOrTokenId().toStringUtf8();
    String typeStr = event.getType().toStringUtf8();
    String mainChainAddressStr = WalletUtil
        .encode58Check(event.getMainchainAddress().toByteArray());
    String userSignStr = ByteArray.toHexString(event.getUserSign().toByteArray());
    String dataHashStr = ByteArray.toHexString(event.getDataHash().toByteArray());
    String originalTransactionIdStr = ByteArray
        .toHexString(event.getOriginalTransactionId().toByteArray());
    String transactionIdStr = ByteArray.toHexString(event.getTransactionId().toByteArray());

    logger.info(
        "MultiSignForWithdrawTokenActuator, from: {}, mainChainAddress: {}, valueOrTokenId: {}, "
            + "_type: {}, userSign: {}, dataHash: {}, originalTransactionId: {}, transactionId: {}",
        fromStr,
        mainChainAddressStr, valueOrTokenIdStr, typeStr, userSignStr, dataHashStr,
        originalTransactionIdStr,
        transactionIdStr);
    Transaction tx = MainChainGatewayApi
        .multiSignForWithdrawTokenTransaction(fromStr, mainChainAddressStr, valueOrTokenIdStr,
            typeStr,
            userSignStr, dataHashStr, originalTransactionIdStr);
    if (tx == null) {
      return null;
    }
    this.transactionExtensionCapsule = new TransactionExtensionCapsule(TaskEnum.MAIN_CHAIN,
        transactionIdStr, tx);
    return this.transactionExtensionCapsule;
  }

  @Override
  public EventMsg getMessage() {
    return EventMsg.newBuilder().setParameter(Any.pack(this.event)).setType(getType()).build();
  }

  @Override
  public byte[] getKey() {
    return event.getTransactionId().toByteArray();
  }

  @Override
  public byte[] getNonce() {
    return null;
  }
}