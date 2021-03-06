package com.blazemeter.jmeter.rte.protocols.tn5250;

import static org.assertj.core.api.Assertions.assertThat;

import com.blazemeter.jmeter.rte.core.Input;
import com.blazemeter.jmeter.rte.core.InvalidFieldLabelException;
import com.blazemeter.jmeter.rte.core.LabelInput;
import com.blazemeter.jmeter.rte.core.*;
import com.blazemeter.jmeter.rte.core.ssl.SSLContextFactory;
import com.blazemeter.jmeter.rte.core.ssl.SSLType;
import com.blazemeter.jmeter.rte.core.wait.Area;
import com.blazemeter.jmeter.rte.core.wait.CursorWaitCondition;
import com.blazemeter.jmeter.rte.core.wait.SilentWaitCondition;
import com.blazemeter.jmeter.rte.core.wait.SyncWaitCondition;
import com.blazemeter.jmeter.rte.core.wait.TextWaitCondition;
import com.blazemeter.jmeter.rte.core.wait.WaitCondition;
import com.blazemeter.jmeter.rte.protocols.RteProtocolClientIT;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;
import org.junit.Test;

public class Tn5250ClientIT extends RteProtocolClientIT<Tn5250Client> {

  @Override
  protected Tn5250Client buildClient() {
    return new Tn5250Client();
  }

  @Override
  protected TerminalType getDefaultTerminalType() {
    return client.getTerminalTypeById("IBM-3477-FC");
  }

  @Test
  public void shouldGetWelcomeScreenWhenConnect() throws Exception {
    loadLoginFlow();
    connectToVirtualService();
    assertThat(client.getScreen())
        .isEqualTo(getFileContent("login-welcome-screen.txt"));
  }

  private void loadLoginFlow() throws FileNotFoundException {
    loadFlow("login-immediate-responses.yml");
  }

  @Test
  public void shouldGetWelcomeScreenWhenConnectWithSsl() throws Exception {
    server.stop(SERVER_STOP_TIMEOUT);
    loadLoginFlow();
    SSLContextFactory.setKeyStore(findResource("/.keystore").getFile());
    SSLContextFactory.setKeyStorePassword("changeit");
    server.setSslEnabled(true);
    server.start();
    client.connect(VIRTUAL_SERVER_HOST, server.getPort(), SSLType.TLS, getDefaultTerminalType(),
        TIMEOUT_MILLIS, STABLE_TIMEOUT_MILLIS);
    assertThat(client.getScreen())
        .isEqualTo(getFileContent("login-welcome-screen.txt"));
  }

  @Test(expected = RteIOException.class)
  public void shouldThrowRteIOExceptionWhenConnectWithInvalidPort() throws Exception {
    client.connect(VIRTUAL_SERVER_HOST, 1, SSLType.NONE, getDefaultTerminalType(), TIMEOUT_MILLIS,
        STABLE_TIMEOUT_MILLIS);
  }

  @Test(expected = TimeoutException.class)
  public void shouldThrowTimeoutExceptionWhenConnectAndServerIsTooSlow() throws Exception {
    loadFlow("slow-welcome-screen.yml");
    connectToVirtualService();
  }

  @Test
  public void shouldGetUserMenuScreenWhenSendCredsByCoord() throws Exception {
    loadFlow("login.yml");
    connectToVirtualService();
    sendCredsByCoordWithSyncWait();
    assertThat(client.getScreen())
        .isEqualTo(getFileContent("user-menu-screen.txt"));
  }

  private void sendCredsByCoordWithSyncWait() throws Exception {
    client.send(buildCredsFieldsByCoord(), AttentionKey.ENTER);
    client.await(
        Collections.singletonList(new SyncWaitCondition(TIMEOUT_MILLIS, STABLE_TIMEOUT_MILLIS)));
  }

  private List<Input> buildCredsFieldsByCoord() {
    return Arrays.asList(
        new CoordInput(new Position(6, 53), "TESTUSR"),
        new CoordInput(new Position(7, 53), "TESTPSW"));
  }

  @Test
  public void shouldGetUserMenuScreenWhenSendCredsByLabel() throws Exception {
    loadFlow("login.yml");
    connectToVirtualService();
    sendCredsByLabelWithSyncWait();
    assertThat(client.getScreen())
            .isEqualTo(getFileContent("user-menu-screen.txt"));
  }

  private void sendCredsByLabelWithSyncWait() throws Exception {
    client.send(buildCredsFieldsByLabel(), AttentionKey.ENTER);
    client.await(
            Collections.singletonList(new SyncWaitCondition(TIMEOUT_MILLIS, STABLE_TIMEOUT_MILLIS)));
  }

  private List<Input> buildCredsFieldsByLabel() {
    return Arrays.asList(
            new LabelInput("User", "TESTUSR"),
            new LabelInput("Password", "TESTPSW"));
  }

  @Test(expected = InvalidFieldPositionException.class)
  public void shouldThrowInvalidFieldPositionExceptionWhenSendIncorrectFieldPositionByCoord()
      throws Exception {
    loadLoginFlow();
    connectToVirtualService();
    List<Input> input = Collections.singletonList(
        new CoordInput(new Position(7, 1), "TESTUSR"));
    client.send(input, AttentionKey.ENTER);
  }

  @Test(expected = InvalidFieldLabelException.class)
  public void shouldThrowInvalidFieldPositionExceptionWhenSendIncorrectFieldPositionByLabel()
          throws Exception {
    loadLoginFlow();
    connectToVirtualService();
    List<Input> input = Collections.singletonList(
            new LabelInput("Usr", "TESTUSR"));
    client.send(input, AttentionKey.ENTER);
  }

  @Test
  public void shouldGetTrueSoundAlarmWhenServerSendTheSignal() throws Exception {
    loadLoginFlow();
    connectToVirtualService();
    sendCredsByCoordWithSyncWait();
    assertThat(client.getSoundAlarm()).isTrue();
  }

  @Test
  public void shouldGetFalseSoundAlarmWhenServerDoNotSendTheSignal() throws Exception {
    loadLoginFlow();
    connectToVirtualService();
    assertThat(client.getSoundAlarm()).isFalse();
  }

  @Test(expected = RteIOException.class)
  public void shouldThrowRteIOExceptionWhenSendAndServerDown() throws Exception {
    loadLoginFlow();
    connectToVirtualService();
    server.stop(SERVER_STOP_TIMEOUT);
    sendCredsByCoordWithSyncWait();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void shouldThrowUnsupportedOperationExceptionWhenAwaitWithUndefinedCondition()
      throws Exception {
    loadLoginFlow();
    connectToVirtualService();
    List<WaitCondition> conditions = Collections
        .singletonList(new WaitCondition(TIMEOUT_MILLIS, STABLE_TIMEOUT_MILLIS) {
          @Override
          public String getDescription() {
            return "test";
          }
        });
    client.await(conditions);
  }

  @Test(expected = TimeoutException.class)
  public void shouldThrowTimeoutExceptionWhenSyncWaitAndSlowResponse() throws Exception {
    loadFlow("slow-response.yml");
    connectToVirtualService();
    client.send(buildCredsFieldsByCoord(), AttentionKey.ENTER);
    client.await(
        Collections.singletonList(new SyncWaitCondition(TIMEOUT_MILLIS, STABLE_TIMEOUT_MILLIS)));
  }

  @Test(expected = TimeoutException.class)
  public void shouldThrowTimeoutExceptionWhenCursorWaitAndNotExpectedCursorPosition()
      throws Exception {
    loadLoginFlow();
    connectToVirtualService();
    client.send(buildCredsFieldsByCoord(), AttentionKey.ENTER);
    client.await(Collections.singletonList(
        new CursorWaitCondition(new Position(1, 1), TIMEOUT_MILLIS, STABLE_TIMEOUT_MILLIS)));
  }

  @Test(expected = TimeoutException.class)
  public void shouldThrowTimeoutExceptionWhenSilentWaitAndChattyServer() throws Exception {
    loadFlow("chatty-server.yml");
    connectToVirtualService();
    client.send(buildCredsFieldsByCoord(), AttentionKey.ENTER);
    client.await(
        Collections.singletonList(new SilentWaitCondition(TIMEOUT_MILLIS, STABLE_TIMEOUT_MILLIS)));
  }

  @Test(expected = TimeoutException.class)
  public void shouldThrowTimeoutExceptionWhenTextWaitWithNoMatchingRegex()
      throws Exception {
    loadLoginFlow();
    connectToVirtualService();
    client.send(buildCredsFieldsByCoord(), AttentionKey.ENTER);
    client.await(Collections
        .singletonList(new TextWaitCondition(new Perl5Compiler().compile("testing-wait-text"),
            new Perl5Matcher(),
            Area.fromTopLeftBottomRight(1, 1, Position.UNSPECIFIED_INDEX,
                Position.UNSPECIFIED_INDEX),
            TIMEOUT_MILLIS,
            STABLE_TIMEOUT_MILLIS)));
  }

  @Test
  public void shouldGetWelcomeScreenWhenConnectAfterDisconnect() throws Exception {
    loadLoginFlow();
    connectToVirtualService();
    sendCredsByCoordWithSyncWait();
    client.disconnect();
    connectToVirtualService();
    assertThat(client.getScreen())
        .isEqualTo(getFileContent("login-welcome-screen.txt"));
  }

  @Test
  public void shouldNotThrowExceptionWhenDisconnectAndServerDown() throws Exception {
    loadLoginFlow();
    connectToVirtualService();
    server.stop(SERVER_STOP_TIMEOUT);
    client.disconnect();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void shouldThrowUnsupportedOperationExceptionWhenSelectAttentionKeyUnsupported()
      throws Exception {
    loadLoginFlow();
    connectToVirtualService();
    client.send(buildCredsFieldsByCoord(), AttentionKey.PA1);
  }

}
