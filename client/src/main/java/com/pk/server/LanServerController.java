package com.pk.server;

import com.pk.server.exceptions.InvitationRejected;
import com.pk.server.exceptions.MoveRejected;
import com.pk.server.models.Invite;
import com.pk.server.models.Move;
import com.pk.server.models.Player;
import java.io.IOException;
import java.net.DatagramSocket;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import lombok.Getter;

/** */
public class LanServerController implements ServerController {

  private ProbeResponder pResponder;
  private UdpServer udpServer;
  private LocalTcpServer tcpServer;
  // FIXME threads num shouldn't be magic number
  private ExecutorService executorService = Executors.newFixedThreadPool(2);

  private @Getter Future<Integer> futureResponder;
  private @Getter Future<Integer> futureTcp;

  /**
   * @param bQueue queue which will contain all received invites
   * @param ip ip to bind
   * @param port port to bind
   * @param nick current player nickname
   * @param profileImg current player profileImg b64 encoded
   * @throws IOException if underling channel is closed.
   */
  public LanServerController(
      BlockingQueue<Invite> bQueue, String ip, Integer port, String nick, String profileImg)
      throws IOException {
    udpServer = new BasicUdpServer(new DatagramSocket(port));
    tcpServer = new LanTcpServer(bQueue, ip, port, new HashMap<>());
    pResponder = new BasicProbeResponder(nick, profileImg);
    futureResponder = executorService.submit(pResponder);
    futureTcp = executorService.submit(tcpServer);
  }

  @Override
  public void move(Move move) throws IOException, MoveRejected {
    tcpServer.move(move);
  }

  @Override
  public void chatSendMsg(String msg) throws IOException {
    tcpServer.chatSendMsg(msg);
  }

  @Override
  public Future<List<Player>> getActivePlayers() throws IOException {
    return udpServer.getActivePlayers();
  }

  @Override
  public Future<Boolean> invite(String inviteCode) throws InvitationRejected, IOException {
    return tcpServer.invite(inviteCode);
  }

  @Override
  public boolean acceptInvitation(String inviteCode) throws IOException {
    return tcpServer.acceptInvitation(inviteCode);
  }
}
