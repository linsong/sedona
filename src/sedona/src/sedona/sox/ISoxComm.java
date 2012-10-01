package sedona.sox;

import java.util.Hashtable;
import java.util.Properties;

import sedona.dasp.DaspSession;

public interface ISoxComm
{
  /**
   * Send a single message and do not wait for any response.
   */
  void send(Msg buf)
    throws Exception;

  /**
   * Send a single request and wait for the response.
   */
  Msg request(Msg req)
    throws Exception;

  /**
   * Send a batch of requests and wait for the responses.
   * @param req array of requests to send
   * @return the corresponding array of responses
   * @throws Exception
   */
  Msg[] request(Msg[] req)
    throws Exception;

  /**
   * Connect to the remote Sedona server using the parameters
   * passed to the constructor.
   * @param options
   * @throws Exception
   */
  void connect(Hashtable options)
    throws Exception;

  /**
   * Return the SoxClient for interpreting the object model.
   */
  SoxClient client();
  
  /**
   * Return the underlying DaspSession or null if closed.
   */
  DaspSession session();

  /**
   * Return the local sessionId or -1 if closed.
   */
  int localId();

  /**
   * Return the remote sessionId or -1 if closed.
   */
  int remoteId();

  /**
   * Is this session currently closed?
   */
  boolean isClosed();

  /**
   * Close this session.<p>
   * If this comm is implemented with a session-queued DaspSocket,
   * this call will close the DaspSession.<p>
   * If it is backed by a socket-queued DaspSession, it will leave
   * the session open, as it may be used by multiple entities.
   * The socket-queued implementation is responsible for cleaning
   * up any unused sessions after a reasonable amount of time. 
   */
  void close();

  /**
   * Read the specified URI into the given file with the
   * specified headers.  Return the response headers.
   * Standard headers:
   *   - chunkSize: client's preference for chunk size in bytes
   */
  Properties getFile(String           uri,
                     SoxFile          file,
                     Properties       headers,
                     TransferListener listener)
    throws Exception;

  /**
   * Write the file specified URI using the contents of the given SoxFile
   * with the specified headers.  Return the response headers.
   * Standard headers:
   *   - chunkSize: client's preference for chunk size in bytes
   *   - staged: true to put as staged file (defaults to false)
   */
  Properties putFile(String           uri,
                     SoxFile          file,
                     Properties       headers,
                     TransferListener listener)
    throws Exception;

  /**
   * Does this ISoxComm have an underlying subscription for this
   * SoxComponent?
   * @param c
   * @return true if a subscription exists
   */
  boolean isSubscribed(SoxComponent c);
  

//////////////////////////////////////////////////////////////////////////
// TransferListener
//////////////////////////////////////////////////////////////////////////

  public static interface TransferListener
  {
    public void progress(int bytesTransfered, int bytesTotal);
  }


}
