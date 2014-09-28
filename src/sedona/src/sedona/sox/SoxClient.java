package sedona.sox;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.util.*;

import sedona.*;
import sedona.dasp.DaspSession;
import sedona.dasp.DaspSocket;
import sedona.manifest.KitManifest;
import sedona.manifest.ManifestDb;
import sedona.manifest.ManifestZipUtil;
import sedona.sox.ISoxComm.TransferListener;
import sedona.util.Version;

/**
 * SoxClient implements the client side functionality
 * of Sox for a Java VM.
 */
public class SoxClient
{

//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////

  /**
   * Constructor
   */
  public SoxClient(DaspSocket socket, InetAddress addr, int port, String username, String password)
  {
    this.socket   = socket;
    this.addr     = addr;
    this.port     = port;
    this.username = username;
    this.password = password;
    this.util  = new SoxUtil(this);
    initOptions();
  }

//////////////////////////////////////////////////////////////////////////
// Lifecycle
// These methods are provided as convenience methods wrapping
// access to the underlying ISoxComm.
//////////////////////////////////////////////////////////////////////////

  /**
   * Convenience access to my <code>SoxExchange</code>.
   * @return a guaranteed non-null reference to this client's <code>SoxExchange</code>.
   */
  ISoxComm comm()
  {
    if (comm == null) comm = new SoxExchange(this);
    return comm;
  }

  public void setComm(ISoxComm c)
  {
    comm = c;
  }
  
  /**
   * Convenience for <code>connect(null)</code>.
   */
  public synchronized void connect()
    throws Exception
  {
    comm().connect(null);
  }

  /**
   * Connect to the remote sedona server using the
   * parameters passed to the constructor.
   */
  public synchronized void connect(Hashtable options)
    throws Exception
  {
    comm().connect(options);
  }

  /**
   * Return the underlying DaspSession or null if closed.
   * The DaspSession should never be used directly for messaging.
   */
  public DaspSession session()
  {
    return comm().session();
  }

  /**
   * Return the local session id or -1 if closed.
   */
  public int localId()
  {
    return comm().localId();
  }

  /**
   * Return the remote session id or -1 if closed.
   */
  public int remoteId()
  {
    return comm().remoteId();
  }

  /**
   * Is this session currently closed.
   */
  public boolean isClosed()
  {
    return comm().isClosed();
  }

  /**
   * Close this session.
   */
  public void close()
  {
    comm().close();
  }
  
//////////////////////////////////////////////////////////////////////////
// Read Schema
//////////////////////////////////////////////////////////////////////////

  /**
   * Read the schema version.  If we've already read it
   * for this session then return the cached version.
   */
  public synchronized Schema readSchema()
    throws Exception
  {
    // check cache
    if (util.schema != null)
      return util.schema;

    // build request
    Msg req = Msg.prepareRequest('v');

    // send request
    Msg res = request(req);

    // parse response
    res.checkResponse('V');
    int num = res.u1();
    KitPart[] parts = new KitPart[num];
    for (int i=0; i<num; ++i)
      parts[i] = new KitPart(res.str(), res.i4());

    // load schema - right now if we can't resolve the schema
    // against the local manifest database, then you can't proceed;
    // TODO: eventually we should lazy load the kit/type/slot
    // definitions over the network
    util.schema = loadSchema(parts);
    return util.schema;
  }

  private Schema loadSchema(KitPart[] parts) throws Exception
  {
    try
    {
      return Schema.load(parts);
    }
    catch (Schema.MissingKitManifestException missing)
    {
      tryResolveMissing(missing.parts);
      return Schema.load(parts);
    }
  }
  
  private  void tryResolveMissing(KitPart[] missing) throws Exception
  {
    KitManifest[] resolved = null;
    Buf b = new Buf();
    try
    {
      // first try single zip transfer
      getFile("m:m.zip", SoxFile.make(b), null ,null);
      resolved = ManifestZipUtil.extract(b, missing);
    }
    catch (Exception e)
    {
      // maybe single zip transfer will work...
    }
    
    if (resolved == null)
    {
      // now try single zip transfer
      resolved = new KitManifest[missing.length];
      for (int i=0; i<missing.length; ++i)
      {
        b = new Buf();
        try
        {
          getFile("m:" + missing[i] + ".xml", SoxFile.make(b), null, null);
          resolved[i] = ManifestZipUtil.extract(b, missing[i]);
        }
        catch (Exception e)
        {
          resolved[i] = null;
        }
      }
    }

    for (int i=0; i<resolved.length; ++i)
      if (resolved[i] != null)
        ManifestDb.save(resolved[i]);
  }

  /**
   * Read the full set of version meta-data.  If we've
   * already read it for this session then return the
   * cached version.
   */
  public synchronized VersionInfo readVersion()
    throws Exception
  {
    // check cache
    if (util.version != null)
      return util.version;

    // read schema
    Schema schema = readSchema();

    // build request
    Msg req = Msg.prepareRequest('y');

    // send request
    Msg res = request(req);

    // parse response
    res.checkResponse('Y');
    String platformId = res.str();
    int scodeFlags = res.u1();
    KitVersion[] kits = new KitVersion[schema.kits.length];
    for (int i=0; i<kits.length; ++i)
    {
      Kit part = schema.kits[i];
      Version ver = new Version(res.str());
      kits[i] = new KitVersion(part.name, part.checksum, ver);
    }
    util.version = new VersionInfo(platformId, scodeFlags, kits);
    int propNum = res.u1();
    for (int i=0; i<propNum; ++i)
      util.version.props.put(res.str(), res.str());

    return util.version;
  }

//////////////////////////////////////////////////////////////////////////
// Read Prop (single)
//////////////////////////////////////////////////////////////////////////

  /**
   * Read a property.
   */
  public synchronized Value readProp(SoxComponent comp, Slot slot)
    throws Exception
  {
    checkMine(comp);
    return readProp(comp.id(), slot);
  }

  /**
   * Read a property using raw component id and slot.
   */
  public synchronized Value readProp(int compId, Slot slot)
    throws Exception
  {
    // build request
    Msg req = Msg.prepareRequest('r');
    req.u2(compId);
    req.u1(slot.id);

    // send request
    Msg res = request(req);

    // parse response
    res.checkResponse('R');
    res.u2();     // resCompId
    res.u1();     // resPropId
    int resTypeId = res.u1();

    // return value
    Value v = slot.isAsStr() ? Str.make("") : Value.defaultForType(resTypeId);
    return v.decodeBinary(res);
  }

//////////////////////////////////////////////////////////////////////////
// Load Component (Tree)
//////////////////////////////////////////////////////////////////////////

  /**
   * Return the app root component.
   * Convenience for <code>load(0)</code>.
   */
  public synchronized SoxComponent loadApp()
    throws Exception
  {
    return load(0);
  }

  /**
   * Convenience for <code>load(int[])</code>.
   */
  public synchronized SoxComponent load(int id)
    throws Exception
  {
    if (id < 0) return null;
    if (cache(id) != null) return cache(id);
    return load(new int[] { id })[0];
  }

  /**
   * Convenience for <code>load(int[], true)</code>.
   */
  public synchronized SoxComponent[] load(int[] ids)
    throws Exception
  {
    return load(ids, true);
  }

  /**
   * Load the component meta-data definitions of the specified ids.
   * If we've already loaded a given id return the cached SoxComponent,
   * otherwise perform a network call to read it.  Note the
   * SoxComponent only represents identity and tree structure,
   * none of it's property values or links are fetched.  If checked
   * is true and any ids fails then throw SoxException.  If checked
   * is false then return null for that id in the resulting array.
   */
  public synchronized SoxComponent[] load(int[] ids, boolean checked)
    throws Exception
  {
    readSchema();
    int n = ids.length;
    SoxComponent[] result = new SoxComponent[n];

    // check cache and if not found add to req accumulator
    ArrayList reqs = new ArrayList();
    for (int i=0; i<n; ++i)
    {
      int id = ids[i];
      result[i] = cache(id);
      if (result[i] == null)
        reqs.add(Msg.makeUpdateReq(id, 't'));
    }

    // if reqs is empty then we found them all in the cache
    if (reqs.size() == 0)
      return result;

    // send requests
    Msg[] responses = requests((Msg[])reqs.toArray(new Msg[reqs.size()]));

    // parse responses and apply
    for (int i=0; i<responses.length; ++i)
    {
      Msg res = responses[i];
      if (!checked && res.isError()) continue;
      res.checkResponse('C');
      applyToCache(res);
    }

    // everything should be in app cache now
    for (int i=0; i<n; ++i)
    {
      int id = ids[i];
      result[i] = cache(id);
      if (checked && result[i] == null) throw new IllegalStateException(""+id);
    }
    return result;
  }

//////////////////////////////////////////////////////////////////////////
// Update
//////////////////////////////////////////////////////////////////////////

  /**
   * Convenience for <code>update(SoxComponent[], mask)</code>.
   * Return comp.
   */
  public synchronized SoxComponent update(SoxComponent comp, int mask)
    throws Exception
  {
    update(new SoxComponent[] { comp }, mask);
    return comp;
  }

  /**
   * Perform a series of requests to update the components
   * with their current values.  The mask specifies which
   * information to synchronized (TREE, CONFIG, RUNTIME, LINKS).
   */
  public synchronized void update(SoxComponent[] comps, int mask)
    throws Exception
  {
    checkMine(comps);

    // build requests
    ArrayList reqs = new ArrayList();
    for (int i=0; i<comps.length; ++i)
    {
      int id = comps[i].id;
      if ((mask & SoxComponent.TREE) != 0)    reqs.add(Msg.makeUpdateReq(id, 't'));
      if ((mask & SoxComponent.CONFIG) != 0)  reqs.add(Msg.makeUpdateReq(id, 'c'));
      if ((mask & SoxComponent.RUNTIME) != 0) reqs.add(Msg.makeUpdateReq(id, 'r'));
      if ((mask & SoxComponent.LINKS) != 0)   reqs.add(Msg.makeUpdateReq(id, 'l'));
    }
    if (reqs.size() == 0) return;

    // send requests
    Msg[] responses = requests((Msg[])reqs.toArray(new Msg[reqs.size()]));

    // parse responses and apply
    for (int i=0; i<responses.length; ++i)
    {
      Msg res = responses[i];
      res.checkResponse('C');
      applyToCache(res);
    }
  }

//////////////////////////////////////////////////////////////////////////
// Subscribe
//////////////////////////////////////////////////////////////////////////

  /**
   * Subscribe to all tree events for entire remote Sedona VM.
   */
  public synchronized void subscribeToAllTreeEvents()
    throws Exception
  {
    // necessarily for pre batch-subscribe compatibility
    if (getSoxVersion() == null)
      request(Msg.makeSubscribeReq(0, 'a'));
    else
    {
      Msg allTree = Msg.prepareRequest('s');
      allTree.u1(0xff);
      allTree.u1(0);
      request(allTree);
//      request(Msg.makeSubscribeReq(0xff00, 0));
    }

    allTreeEvents = true;
    for (int i=0; i<cache.length; ++i)
      if (cache[i] != null) cache[i].subscription |= SoxComponent.TREE;
  }

  /**
   * Convenience for <code>subscribe(SoxComponent[], mask)</code>.
   */
  public synchronized void subscribe(SoxComponent comp, int mask)
    throws Exception
  {
    subscribe(new SoxComponent[] { comp }, mask);
  }

  /**
   * Perform a synchronous request to subscribe the components
   * with their current values.  The mask specifies which
   * information to synchronized (TREE, CONFIG, RUNTIME, LINKS).
   * If any component is already subscribed to a specific
   * category then short circuit the network call.
   */
  public synchronized void subscribe(SoxComponent[] comps, int mask)
    throws Exception
  {
    if (getSoxVersion() != null)
      batchSubscribe(comps, mask, 5000);
    else
      doSubscribe(comps, mask);
  }

  /**
   * Perform an asynchronous request to subscribe the components
   * with their current values.  The mask specifies which
   * information to synchronized (TREE, CONFIG, RUNTIME, LINKS).
   * If any component is already subscribed to a specific
   * category then short circuit the network call.
   * <p>
   * Note: If the remote sox server does not support batch subscription,
   * this will route to a non-batched, synchronous implementation.
   */
  public synchronized void subscribeAsync(SoxComponent[] comps, int mask)
    throws Exception
  {
    if (getSoxVersion() != null)
      batchSubscribe(comps, mask, -1);
    else
      doSubscribe(comps, mask);
  }

  /**
   * @return the Version of the sox protocol running on the Sox server, or
   * null if the remote server did not report a sox protocol version.
   */
  private Version getSoxVersion()
  {
    try
    {
      VersionInfo version = readVersion();
      return Version.parse(version.props.getProperty("soxVer"));
    }
    catch (Exception e)
    {
      return null;
    }
  }

  private void batchSubscribe(SoxComponent[] comps, final int mask, final long timeout)
    throws Exception
  {
    if (comps.length > 255)
      throw new SoxException("Cannot subscribe to more than 255 components: '" + comps.length + "'");

    checkMine(comps);

    // filter components that are already subscribed
    ArrayList arr = new ArrayList();
    for (int i=0; i<comps.length; ++i)
    {
      SoxComponent comp = comps[i];
      if ((comp.subscription() & mask) != mask)
        arr.add(comp);
    }
    if (arr.size() == 0) return;

    // build request
    SoxComponent[] toSubscribe = (SoxComponent[])arr.toArray(new SoxComponent[arr.size()]);
    Msg req = Msg.prepareRequest('s');
    req.u1(mask);
    req.u1(toSubscribe.length);
    for (int i=0; i<toSubscribe.length; ++i)
      req.u2(toSubscribe[i].id);

    Msg response = request(req);
    response.checkResponse('S');
    if (timeout < 0) return; // async

    synchronized (subscribeSyncLock)
    {
      boolean[] syncState = new boolean[toSubscribe.length];
      long lastEvent = Env.ticks();
      int remaining = response.u1();
      while ((remaining > 0) &&
             (lastEvent + timeout > Env.ticks()))
      {
        subscribeSyncLock.wait(250);
        for (int i=0; i<toSubscribe.length; ++i)
        {
          // skip components we have already determined to be subscribed
          if (syncState[i]) continue;

          SoxComponent cacheComp = cache(toSubscribe[i].id);
          if (cacheComp == null) continue;
          if ((syncState[i] = ((cacheComp.subscription() & mask) == mask)))
          {
            --remaining;
            lastEvent = System.currentTimeMillis();
          }
        }
      }
    }
  }

  /**
   * Pre sox protocol version implementation of subscribe.  It does not support batch
   * and is synchronous.  This method is only used if the remote server
   * does not have a sox protocol version.
   */
  private void doSubscribe(SoxComponent[] comps, int mask)
    throws Exception
  {
    checkMine(comps);

    // build requests
    ArrayList reqs = new ArrayList();
    for (int i=0; i<comps.length; ++i)
    {
      SoxComponent comp = comps[i];
      int id = comp.id;
      int cur = comp.subscription();
      int req = mask & ~cur;
      if ((req & SoxComponent.TREE) != 0)    reqs.add(Msg.makeSubscribeReq(id, 't'));
      if ((req & SoxComponent.CONFIG) != 0)  reqs.add(Msg.makeSubscribeReq(id, 'c'));
      if ((req & SoxComponent.RUNTIME) != 0) reqs.add(Msg.makeSubscribeReq(id, 'r'));
      if ((req & SoxComponent.LINKS) != 0)   reqs.add(Msg.makeSubscribeReq(id, 'l'));
    }
    if (reqs.size() == 0) return;

    // send requests
    Msg[] responses = requests((Msg[])reqs.toArray(new Msg[reqs.size()]));

    // parse responses and apply
    for (int i=0; i<responses.length; ++i)
    {
      Msg res = responses[i];
      res.checkResponse('S');
      applyToCache(res);
    }

    // update subscription mask on each component
    for (int i=0; i<comps.length; ++i)
      comps[i].subscription |= mask;
  }

//////////////////////////////////////////////////////////////////////////
// Unsubscribe
//////////////////////////////////////////////////////////////////////////

  /**
   * Unsubscribe from all tree events for entire remote Sedona VM.
   */
  public synchronized void unsubscribeToAllTreeEvents()
    throws Exception
  {
    // silently ignore unsubscribes if closed
    if (isClosed()) return;

    // necessary for pre batch-subscribe compatibility
    if (getSoxVersion() == null)
    {
      request(Msg.makeUnsubscribeReq(0, 0xff));
    }
    else
    {
      Msg allTree = Msg.prepareRequest('u');
      allTree.u1(0xff);
      allTree.u1(0);
      request(allTree);
    }

    allTreeEvents = false;
    for (int i=0; i<cache.length; ++i)
      if (cache[i] != null) cache[i].subscription &= ~SoxComponent.TREE;
  }

  /**
   * Convenience for <code>unsubscribe(SoxComponent[], mask)</code>.
   */
  public synchronized void unsubscribe(SoxComponent comp, int mask)
    throws Exception
  {
    unsubscribe(new SoxComponent[] { comp }, mask);
  }

  /**
   * Unsubscribe for changes to the specified component.
   * The mask indicates which categories to unsubscribe.
   */
  public synchronized void unsubscribe(SoxComponent[] comps, int mask)
    throws Exception
  {
    checkMine(comps);

    // silently ignore unsubscribes if closed
    if (isClosed()) return;

    // if we're always subscribed to tree events,
    // then don't bother to include that bit
    if (allTreeEvents) mask &= ~SoxComponent.TREE;

    ArrayList arr = new ArrayList();
    for (int i=0; i<comps.length; ++i)
    {
      if (comm().isSubscribed(comps[i])) continue;
      if ((comps[i].subscription & mask) != 0)
        arr.add(comps[i]);
    }

    if (arr.size() > 0)
    {
      SoxComponent[] toUnsubscribe = (SoxComponent[])arr.toArray(new SoxComponent[arr.size()]);
      if (getSoxVersion() != null)
        batchUnsubscribe(toUnsubscribe, mask);
      else
        doUnsubscribe(toUnsubscribe, mask);
    }

    // update subscription mask on each component
    for (int i=0; i<comps.length; ++i)
      comps[i].subscription &= ~mask;
  }

  private void batchUnsubscribe(SoxComponent[] comps, int mask)
    throws Exception
  {
    Msg req = Msg.prepareRequest('u');
    req.u1(mask);
    req.u1(comps.length);
    for (int i=0; i<comps.length; ++i)
      req.u2(comps[i].id);
    request(req);
  }

  private void doUnsubscribe(SoxComponent[] comps, int mask)
    throws Exception
  {
    Msg[] reqs = new Msg[comps.length];
    for (int i=0; i<comps.length; ++i)
      reqs[i] = Msg.makeUnsubscribeReq(comps[i].id, mask);
    requests(reqs);
  }

//////////////////////////////////////////////////////////////////////////
// Invoke
//////////////////////////////////////////////////////////////////////////

  /**
   * Invoke an action.
   */
  public synchronized void invoke(SoxComponent comp, Slot slot, Value arg)
    throws Exception
  {
    checkMine(comp);
    invoke(comp.id, slot, arg);
  }

  /**
   * Invoke an action using raw component id.
   */
  public synchronized void invoke(int compId, Slot slot, Value arg)
    throws Exception
  {
    if (!Component.testMode) slot.assertValue(arg);

    // build request
    Msg req = Msg.prepareRequest('i');
    req.u2(compId);
    req.u1(slot.id);
    if (arg != null)
      arg.encodeBinary(req);

    // send request
    Msg res = request(req);

    // parse response
    res.checkResponse('I');
  }

//////////////////////////////////////////////////////////////////////////
// Write
//////////////////////////////////////////////////////////////////////////

  /**
   * Write a property.
   */
  public synchronized void write(SoxComponent comp, Slot slot, Value val)
    throws Exception
  {
    checkMine(comp);
    write(comp.id, slot, val);
  }

  /**
   * Write a property using raw component id.
   */
  public synchronized void write(int compId, Slot slot, Value val)
    throws Exception
  {
    if (!Component.testMode) slot.assertValue(val);

    // build request
    Msg req = Msg.prepareRequest('w');
    req.u2(compId);
    req.u1(slot.id);
    val.encodeBinary(req);

    // send request
    Msg res = request(req);

    // parse response
    res.checkResponse('W');
  }

//////////////////////////////////////////////////////////////////////////
// Database Modification
//////////////////////////////////////////////////////////////////////////

  /**
   * Add a new component to an existing parent of the
   * specified type with the initial config property vals.
   */
  public synchronized SoxComponent add(SoxComponent parent, Type type, String name, Value[] configValues)
    throws Exception
  {
    // check name
    Component.assertName(name);

    // check parent
    checkMine(parent);
    if (parent == null || parent.client != this)
      throw new IllegalArgumentException("Invalid parent");
    if (parent.children.length >= Component.maxChildren)
      throw new IllegalArgumentException("Too many children under component");

    // check type
    if (type == null)
      throw new IllegalArgumentException("Add error: invalid type");

    if (type.schema != util.schema)
    {
      KitPart typePart = type.kit.manifest.part();
      Kit remoteKit = util.schema.kit(type.kit.name);
      if (remoteKit == null)
        throw new IllegalArgumentException("Schema does not support type: " + type);
      KitPart remotePart = remoteKit.manifest.part();
      if (!typePart.toString().equals(remotePart.toString()))
        throw new IllegalArgumentException("Type's KitPart does not match client's: " + typePart + " != " + remotePart);
      type = util.schema.type(type.qname);
    }

    // check config props
    Slot[] props = type.configProps();
    if (props.length != configValues.length)
      throw new IllegalArgumentException("Config props don't match type's definition: " + type + " (" + props.length + " != " + configValues.length + ")");
    for (int i=0; i<configValues.length; ++i)
    {
      Slot prop = props[i];
      int tid = configValues[i].typeId();
      if (prop.isAsStr() ? tid != Type.strId : prop.type.id != tid)
        throw new IllegalArgumentException("Config props don't match type's definition: " + type + " (" + props[i].name + " " + props[i].type + " != " + configValues[i].getClass().getName() + ")");
    }

    // build request
    Msg req = Msg.prepareRequest('a');
    req.u2(parent.id);
    req.u1(type.kit.id);
    req.u1(type.id);
    req.str(name);
    for (int i=0; i<configValues.length; ++i)
      configValues[i].encodeBinary(req);

    // send request
    Msg res = request(req);

    // parse response
    res.checkResponse('A');
    int compId = res.u2();

    // update data structures
    SoxComponent comp = new SoxComponent(this, compId, type);
    cacheAdd(comp);
    comp.name = name;
    comp.parent = parent.id;
    comp.setChildren(new int[0]);
    parent.addChild(compId);

    // default component's configs props
    for (int i=0; i<props.length; ++i)
      comp.set(props[i], configValues[i]);

    // return our new baby component
    return comp;
  }

  /**
   * Rename component.
   */
  public synchronized void rename(SoxComponent comp, String newName)
    throws Exception
  {
    // check name
    Component.assertName(newName);

    checkMine(comp);

    // build request
    Msg req = Msg.prepareRequest('n');
    req.u2(comp.id);
    req.str(newName);

    // send request
    Msg res = request(req);

    // parse response
    res.checkResponse('N');

    // update data structures
    comp.name = newName;
  }

  /**
   * Reorder component children.
   */
  public synchronized void reorder(SoxComponent comp, int[] childrenIds)
    throws Exception
  {
    // get safe copy
    int[] ids = (int[])childrenIds.clone();

    // sanity check length
    if (comp.children.length != ids.length)
      throw new IllegalArgumentException("childrenIds.length wrong");

    // make sure same ids are used
    HashMap match = new HashMap(ids.length*3);
    for (int i=0; i<comp.children.length; ++i)
      match.put(new Integer(comp.children[i]), "x");
    for (int i=0; i<ids.length; ++i)
      if (match.remove(new Integer(ids[i])) == null)
        throw new IllegalArgumentException("childrenIds don't match current");
    if (match.size() != 0)
      throw new IllegalArgumentException("childrenIds don't match current");

    // build request
    Msg req = Msg.prepareRequest('o');
    req.u2(comp.id);
    req.u1(ids.length);
    for (int i=0; i<ids.length; ++i)
      req.u2(ids[i]);

    // send request
    Msg res = request(req);

    // parse response
    res.checkResponse('O');

    // update data structures
    comp.children = ids;
  }

  /**
   * Delete component and all its children.
   */
  public synchronized void delete(SoxComponent comp)
    throws Exception
  {
    checkMine(comp);

    // build request
    Msg req = Msg.prepareRequest('d');
    req.u2(comp.id);

    // send request
    Msg res = request(req);

    // parse response
    res.checkResponse('D');

    // remove from parent children ids
    SoxComponent parent = cache(comp.parent);
    if (parent != null) parent.removeChild(comp.id);

    // recursively remove from cache
    cacheRemove(comp);
  }

  /**
   * Add a link to the application.
   */
  public synchronized void link(Link link)
    throws Exception
  {
    link(link, 'a');
  }

  /**
   * Delete a link from the application.
   */
  public synchronized void unlink(Link link)
    throws Exception
  {
    link(link, 'd');
  }

  private synchronized void link(Link link, int cmd)
    throws Exception
  {
    // build request
    Msg req = Msg.prepareRequest('l');
    req.u1(cmd);
    req.u2(link.fromCompId);
    req.u1(link.fromSlotId);
    req.u2(link.toCompId);
    req.u1(link.toSlotId);

    // send request
    Msg res = request(req);

    // parse response
    res.checkResponse('L');
  }

  /**
   * Convenience for {@code links(comp.id)}
   *
   * @see #links(int)
   */
  public synchronized Link[] links(SoxComponent comp)
    throws Exception
  {
    checkMine(comp);
    return links(comp.id);
  }

  /**
   * Get all the links going in to and out of the given component. This method
   * simply returns a snapshot of the current link state for the component, it
   * does not cause any subscription to take place.
   *
   * @param compId
   *          the id of the component to get links for
   * @return a Link[] containing all links going in to and out from the
   *         component.
   */
  public synchronized Link[] links(int compId)
    throws Exception
  {
    Msg req = Msg.prepareRequest('c');
    req.u2(compId);
    req.u1('l');

    Msg res = request(req);

    res.checkResponse('C');
    int resCompId = res.u2();
    if (resCompId != compId)
      throw new SoxException("Response compId '" + resCompId + "' does not match request '" + compId + "'");
    int l = res.u1();
    if (l != 'l')
      throw new SoxException("Response 'what' is not for links: '" + (char)l + "'");

    ArrayList links = new ArrayList();
    while (true)
    {
      Link link = new Link();
      if ((link.fromCompId = res.u2()) == 0xffff) break;

      link.fromSlotId = res.u1();
      link.toCompId   = res.u2();
      link.toSlotId   = res.u1();

      links.add(link);
    }

    return links.size() == 0 ? Link.none : (Link[])links.toArray(new Link[links.size()]);
  }

//////////////////////////////////////////////////////////////////////////
// Query
//////////////////////////////////////////////////////////////////////////

  /**
   * Query for the installed service type.  Return the
   * list of component ids which implement the service or
   * an empty list if service is not installed.
   */
  public synchronized int[] queryService(Type serviceType)
    throws Exception
  {
    // build request
    Msg req = Msg.prepareRequest('q');
    req.u1('s');
    req.u1(serviceType.kit.id);
    req.u1(serviceType.id);

    // send request
    Msg res = request(req);

    // parse response
    res.checkResponse('Q');
    int[] temp = new int[256];
    int n = 0;
    while (true)
    {
      int id = res.u2();
      if (id == 0xffff) break;
      temp[n++] = id;
    }

    // trim int array
    int[] result = new int[n];
    System.arraycopy(temp, 0, result, 0, n);
    return result;
  }

//////////////////////////////////////////////////////////////////////////
// File Transfer
//////////////////////////////////////////////////////////////////////////

  /**
   * Read the specified URI into the given file with the
   * specified headers.  Return the response headers.
   * Standard headers:
   *   - chunkSize: client's preference for chunk size in bytes
   */
  public synchronized Properties getFile(String uri, SoxFile file,
                                         Properties headers,
                                         TransferListener listener)
    throws Exception
  {
    return comm().getFile(uri, file, headers, listener);
    }

  /**
   * Write the file specified URI using the contents of the given SoxFile
   * with the specified headers.  Return the response headers.
   * Standard headers:
   *   - chunkSize: client's preference for chunk size in bytes
   *   - staged: true to put as staged file (defaults to false)
   */
  public synchronized Properties putFile(String uri, SoxFile file,
                                         Properties headers,
                                         TransferListener listener)
    throws Exception
  {
    return comm().putFile(uri, file, headers, listener);
    }


  /**
   * Rename a file on the remote device.
   */
  public synchronized void renameFile(String from, String to)
    throws Exception
  {
    // build request
    Msg req = Msg.prepareRequest('b');
    req.str(from);
    req.str(to);

    // send request
    Msg res = request(req);

    // parse response
    res.checkResponse('B');
  }


//////////////////////////////////////////////////////////////////////////
// PStore Convenience
//////////////////////////////////////////////////////////////////////////

  /**
   * Return a PstoreFile into a memory buffer.
   */
  public Buf readPstoreFile(SoxComponent pstoreFile)
    throws Exception
  {
    // read filename from parent service
    SoxComponent pstoreService = load(pstoreFile.parent);
    String filename = "";
    try
    {
      filename = readProp(pstoreService, pstoreService.slot("filename")).toString();
    }
    catch (Exception e)
    {
      throw new Exception("PstoreFile parent not PstoreService: " + pstoreService.type);
    }

    // check status
    if (pstoreFile.getInt("status") != 0)
      throw new Exception("PstoreFile.status not ok");

    // get reservation
    int offset = pstoreFile.getInt("resvOffset");
    int size = pstoreFile.getInt("resvSize");

    // read file slice
    Buf buf = new Buf(size);
    Properties headers = new Properties();
    headers.put("fileSize", Integer.toString(size));
    headers.put("offset", Integer.toString(offset));
    getFile(filename, SoxFile.make(buf), headers, null);
    if (buf.size != size)
      throw new IOException("Didn't read all of pstore: " + size + " != " + buf.size);
    return buf;
  }

//////////////////////////////////////////////////////////////////////////
// Apply
//////////////////////////////////////////////////////////////////////////

  public void applyToCache(Msg msg)
    throws Exception
  {
    applyToCache(msg, true);
  }
  
  public void applyToCache(Msg msg, boolean applyMask)
    throws Exception
  {
    final boolean isEvent = msg.command() == 'e';

    int compId = msg.u2();
    int what = msg.u1();
    SoxComponent cached = cache(compId);
    SoxComponent sc = util.apply(msg, compId, what, cached);
    if (cached == null)
      cacheAdd(sc);

    if (isEvent && applyMask)
      applyEvent(sc, what);
  }

  private void applyEvent(SoxComponent sc, final int what)
  {
    if (sc ==  null) return;
    int mask = 0;
    switch (what)
    {
      case 't':
        mask = SoxComponent.TREE; break;
      case 'c':
      case 'C':
        mask = SoxComponent.CONFIG; break;
      case 'r':
      case 'R':
        mask = SoxComponent.RUNTIME; break;
      case 'l':
        mask = SoxComponent.LINKS; break;
      default: throw new IllegalStateException("Unknown event: " + (char)what);
    }
    if ((sc.subscription & mask) == 0)
    {
      synchronized (subscribeSyncLock)
      {
        sc.subscription |= mask;
        subscribeSyncLock.notifyAll();
      }
    }
  }

//////////////////////////////////////////////////////////////////////////
// Cache Management
//////////////////////////////////////////////////////////////////////////

  /**
   * Try to lookup a component in the cache by its id.
   * This will return null if the component is not in
   * the cache, and will NOT attempt to retrieve it from
   * the device.
   */
  public SoxComponent cache(int id)
  {
    if (0 <= id && id < cache.length)
      return cache[id];
    else
      return null;
  }

  /**
   * Add to the cache table.
   */
  void cacheAdd(SoxComponent c)
  {
    if (c.id >= cache.length)
    {
      SoxComponent[] temp = new SoxComponent[Math.max(cache.length*2, c.id+32)];
      System.arraycopy(cache, 0, temp, 0, cache.length);
      cache = temp;
    }
    cache[c.id] = c;
  }

  /**
   * Recursively remove this component and all its descendants
   * from the cache.  This actually won't handle clearing the
   * cache in all cases, but it should nuke old entries in most
   * scenarios.
   */
  void cacheRemove(SoxComponent c)
  {
    int[] children = c.childrenIds();
    for (int i=0; i<children.length; ++i)
    {
      SoxComponent kid = cache(children[i]);
      if (kid != null) cacheRemove(kid);
    }
    cache[c.id] = null;
  }

  /**
   * Check that the SoxComponent.client is me.
   */
  void checkMine(SoxComponent c)
  {
    if (c.client != this)
      throw new IllegalArgumentException("SoxComponent.client != this client");
  }

  /**
   * Check that all the SoxComponent.clients are me.
   */
  void checkMine(SoxComponent[] c)
  {
    for (int i=0; i<c.length; ++i)
      checkMine(c[i]);
  }

//////////////////////////////////////////////////////////////////////////
// Networking
//////////////////////////////////////////////////////////////////////////

  /**
   * Send a single request and wait for the response.
   */
  Msg request(Msg req)
    throws Exception
  {
    return comm().request(new Msg[] { req})[0];
  }

  /**
   * Send a batch of requests and wait for the responses.
   */
  Msg[] requests(Msg[] reqs)
    throws Exception
  {
    //checkOpen(); is done in SoxExchange
    return comm().request(reqs);
  }

//////////////////////////////////////////////////////////////////////////
// Listeners
//////////////////////////////////////////////////////////////////////////

  public static interface Listener
  {
    public void soxClientClosed(SoxClient client);
  }

  public void addListener(Listener listener)
  {
    synchronized (listeners)
    {
      listeners.add(listener);
    }
  }
  
  public void removeListener(Listener listener)
  {
    synchronized (listeners)
    {
      listeners.remove(listener);
    }
  }

  public void closed()
  {
    synchronized (listeners)
    {
      for (int i=0; i<listeners.size(); i++)
      {
        ((Listener)listeners.get(i)).soxClientClosed(this);
      }
    }
  }
  
////////////////////////////////////////////////////////////////
// Tuning Options
////////////////////////////////////////////////////////////////

  /** dump sends/receives */
  public boolean traceMsg = false;

  /** dump stats when file transfer completes */
  public boolean traceXferStats = false;

  public void initOptions()
  {
    try
    {
      traceMsg       = Env.getProperty("sox.traceMsg", traceMsg);
      traceXferStats = Env.getProperty("sox.xfer.traceStats", traceXferStats);
    }
    catch (Throwable e)
    {
      e.printStackTrace();
    }
  }

  public void printOptions() { printOptions(new PrintWriter(System.out)); }
  public void printOptions(PrintWriter out)
  {
    out.println("  traceMsg        = " + traceMsg);
    out.println("  xfer.traceStats = " + traceXferStats);
    out.flush();
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  public final DaspSocket socket;
  public final InetAddress addr;
  public final int port;
  public final String username;
  final String password;
  private Vector listeners = new Vector();
  
  boolean allTreeEvents;
  SoxComponent[] cache = new SoxComponent[1024];

  ISoxComm comm;

  SoxUtil util;
  private final Object subscribeSyncLock = new Object();

}
