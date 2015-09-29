require ("bit")

dasp_proto = Proto("dasp", "Dasp Protocol")
sox_proto = Proto("sox", "Sox Protocol")

dasp_proto.prefs["udp_port"] = Pref.uint("UDP Port", 1876, "UDP Port for Dasp")

-- dasp fields
local f_sessionId = ProtoField.uint16("dasp.sessionId", "SessionId", base.HEX)
local f_seqNum = ProtoField.uint16("dasp.seqNum", "seqNum", base.DEC)
local f_msgType = ProtoField.uint16("dasp.msgType", "msgType", base.HEX)
local f_headerFieldNum = ProtoField.uint16("dasp.numFields", "numFields", base.DEC)
-- local f_headerId = ProtoField.uint8("dasp.headerId", "headerId", base.HEX)
-- local f_headerDataType = ProtoField.uint8("dasp.headerDataType", "headerDataType", base.HEX)
local f_headerU2Val = ProtoField.uint16("dasp.headerU2Val", "headerU2Val", base.Hex)
local f_headerStrVal = ProtoField.stringz("dasp.headerStrVal", "headerStrVal")
local f_headerByteVal = ProtoField.bytes("dasp.headerByteVal", "headerByteVal")

dasp_proto.fields = {f_sessionId, f_seqNum, f_msgType, f_headerFieldNum, 
                     -- f_headerId, f_headerDataType, 
                     f_headerU2Val, f_headerStrVal, f_headerByteVal}

-- sox fields
local f_soxCmd = ProtoField.string("sox.cmd", "cmd")
local f_soxReplyNum = ProtoField.uint8("sox.replyNum", "replyNum", base.HEX)
local f_soxCompId = ProtoField.uint16("sox.compId", "compId", base.DEC)
local f_soxSlotId = ProtoField.uint8("sox.slotId", "slotId", base.DEC)

local f_parentCompId = ProtoField.uint16("sox.parentCompId", "parentCompId", base.DEC)
local f_kitId = ProtoField.uint8("sox.kitId", "kitId", base.DEC)
local f_typeId = ProtoField.uint8("sox.typeId", "typeId", base.DEC)
local f_compName = ProtoField.stringz("sox.compName", "compName")
local f_compWhat = ProtoField.string("sox.compWhat", "compWhat")

local f_linkAction = ProtoField.uint8("sox.linkAction", "linkAction", base.HEX)

local f_linkFromCompId = ProtoField.uint16("sox.linkFromCompId", "linkFromCompId", base.DEC)
local f_linkFromSlotId = ProtoField.uint8("sox.linkFromSlotId", "linkFromSlotId", base.DEC)
local f_linkToCompId = ProtoField.uint16("sox.linkToCompId", "linkToCompId", base.DEC)
local f_linkToSlotId = ProtoField.uint8("sox.linkToSlotId", "linkToSlotId", base.DEC)

local f_fileOpenMethod = ProtoField.string("sox.fileOpenMethod", "fileOpenMethod")
local f_fileUri = ProtoField.stringz("sox.fileUri", "fileUri")
local f_fileSize = ProtoField.uint32("sox.fileSize", "fileSize", base.DEC)

local f_chunkNum = ProtoField.uint16("sox.chunkNum", "chunkNum", base.DEC)
local f_chunkSize = ProtoField.uint16("sox.chunkSize", "chunkSize", base.DEC)

local f_soxBytes = ProtoField.bytes("sox.byteVal", "bytesVal")
local f_soxStr = ProtoField.stringz("sox.strVal", "strVal")
local f_soxPlatformId = ProtoField.stringz("sox.platformId", "platformId")
local f_soxError = ProtoField.stringz("sox.errorStr", "errorStr")

sox_proto.fields = {f_soxCmd, f_soxReplyNum, f_soxCompId, f_soxSlotId, 
                    f_parentCompId, f_kitId, f_typeId, f_compName, 
                    f_compWhat, f_linkAction, f_linkFromCompId,
                    f_linkFromSlotId, f_linkToCompId, f_linkToSlotId, f_chunkNum,
                    f_chunkSize, f_soxBytes, f_soxStr, f_soxPlatformId, f_soxError}

local msg_types = {
  [0] = {"discover", "Discover"},
  [1] = {"hello", "Hello"},
  [2] = {"challenge", "Challenge"},
  [3] = {"authenticate", "Authenticate"},
  [4] = {"welcome", "Welcome"},
  [5] = {"keepAlive", "KeepAlive"},
  [6] = {"datagram", "Datagram"},
  [7] = {"close", "Close"}
};

local header_mappings = {
  [0x05] = "version",
  [0x09] = "remoteId",
  [0x0e] = "digestAlgorithm",
  [0x13] = "nonce",
  [0x16] = "username",
  [0x1b] = "digest",
  [0x1d] = "idealMax",
  [0x21] = "absMax",
  [0x25] = "ack",
  [0x2b] = "ackMore",
  [0x2d] = "receiveMax",
  [0x31] = "receiveTimeout",
  [0x35] = "errorCode",
  [0x3a] = "platformId"
};

local header_ids = {
  [1] = "version",
  [2] = "remoteId",
  [3] = "digestAlgorithm",
  [4] = "nonce",
  [5] = "username",
  [6] = "digest",
  [7] = "idealMax",
  [8] = "absMax",
  [9] = "ack",
  [10] = "ackMore",
  [11] = "receiveMax",
  [12] = "receiveTimeout",
  [13] = "errorCode",
  [14] = "platformId"
};

local header_types = {
  [0] = {"nil", "No header data"},
  [1] = {"u2", "uint2"},
  [2] = {"str", "string"},
  [3] = {"bytes", "raw bytes"}
};

local error_codes = {
  [0xe1] = "incompatibleVersion",
  [0xe2] = "busy",
  [0xe3] = "digestNotSupported",
  [0xe4] = "notAuthenticated",
  [0xe5] = "timeout"
};

local sox_cmds = {
  ["a"] = "add",
  ["b"] = "fileRename",
  ["c"] = "readComp",
  ["d"] = "delete",
  ["e"] = "event",
  ["f"] = "fileOpen",
  ["i"] = "invoke",
  ["k"] = "fileChunk",
  ["l"] = "link",
  ["n"] = "rename",
  ["o"] = "reorder",
  ["q"] = "query",
  ["r"] = "readProp",
  ["s"] = "subscribe",
  ["u"] = "unsubscribe",
  ["v"] = "version",
  ["w"] = "write",
  ["y"] = "versionMore",
  ["z"] = "fileClose",
  ["!"] = "error"
};

function add_string(tree, field, buf, offset)
  local str = buf(offset):stringz("utf-8")
  tree:add(field, buf(offset, string.len(str) + 1), str)
  offset = offset + string.len(str) + 1
  return offset
end

function add_compId(tree, buf, offset)
  tree:add(f_soxCompId, buf(offset, 2))
  offset = offset + 2
  return offset
end

function add_id_list(num, tree, buf, offset)
  local compIds = {}
  for i=1, num do
    table.insert(compIds, buf(offset+(i-1)*2, 2):uint())
  end
  tree:add(buf(offset, 2*num), "compIds", table.concat(compIds, ","))
  offset = offset + 2*num
  return offset
end

function add_whatMask(tree, buf, offset)
  local mask = buf(offset, 1):uint()
  local maskElem = tree:add(buf(offset, 1), "whatMask", string.format("0x%02x", mask))
  offset = offset + 1
  if mask == 0xff then 
    maskElem:append_text("(all tree)")
  else
    local parts = {}
    if bit.band(mask, 0x01) then table.insert(parts, "tree") end
    if bit.band(mask, 0x02) then table.insert(parts, "config") end
    if bit.band(mask, 0x04) then table.insert(parts, "runtime") end
    if bit.band(mask, 0x08) then table.insert(parts, "links") end
    maskElem:append_text("(" .. table.concat(parts, "|") .. ")")
  end

  return offset
end

function add_file_headers(tree, buf, offset)
  local byte = buf(offset, 1)
  while byte ~= '\0' do 
    local start = offset
    local name = buf(offset):stringz("utf-8")
    offset = offset + string.len(name) + 1

    local value = buf(offset):stringz("utf-8")
    offset = offset + string.len(value) + 1
    tree:add(buf(start, offset-start), name, value)

    byte = buf(offset, 1)
  end
  offset = offset + 1

  return offset
end

function parse_comp_tree(tree, buf, offset)
  local subtree = tree:add(f_compWhat, buf(offset, 1), buf(offset, 1):string(), "Tree")
  offset = offset + 1

  subtree:add(f_kitId, buf(offset, 1))
  offset = offset + 1

  subtree:add(f_typeId, buf(offset, 1))
  offset = offset + 1

  offset = add_string(subtree, f_compName, buf, offset)

  subtree:add(f_parentCompId, buf(offset, 2))
  offset = offset + 2
  
  subtree:add(buf(offset, 1), "permissions", string.format("0x%02x", buf(offset, 1):uint()))
  offset = offset + 1

  local numKids = buf(offset, 1):uint()
  subtree:add(buf(offset, 1), "numKids", numKids)
  offset = offset + 1
  
  offset = add_id_list(numKids, subtree, buf, offset)

  return offset
end

function parse_comp_links(tree, buf, offset)
  local subtree = tree:add(f_compWhat, buf(offset, 1), buf(offset, 1):string(), "Links")
  offset = offset + 1
  
  local index = 1
  local fromCompId = buf(offset, 2):uint()
  while fromCompId ~= 0xffff do
    local fromSlotId = buf(offset+2, 1):uint()
    local toCompId = buf(offset+3, 2):uint()
    local toSlotId = buf(offset+5, 1):uint()
    
    local label = "" .. fromCompId .. "." .. fromSlotId .. " -> " .. toCompId .. "." .. toSlotId
    subtree:add(buf(offset, 6), "Link" .. index, label)
    offset = offset + 6
    index = index + 1

    fromCompId = buf(offset, 2):uint()
  end 
  offset = offset + 2

  return offset
end

function parse_comp_props(tree, buf, offset)
  local typeChar = buf(offset, 1):uint()
  local subtree = nil
  if typeChar == 'c' or typeChar == 'C' then
    subtree = tree:add(f_compWhat, buf(offset, 1), buf(offset, 1):string(), "Config Props")
  elseif typeChar == 'r' or typeChar == 'R' then 
    subtree = tree:add(f_compWhat, buf(offset, 1), buf(offset, 1):string(), "Runtime Props")
  end
  offset = offset + 1

  return offset
end

local sox_handlers = {
  -- add comp
  ["a"] = function(tree, buf, offset)
    tree:add(f_parentCompId, buf(offset, 2))
    offset = offset + 2

    tree:add(f_kitId, buf(offset, 1))
    offset = offset + 1

    tree:add(f_typeId, buf(offset, 1))
    offset = offset + 1
    
    offset = add_string(tree, f_compName, buf, offset)

    -- TODO: handle configVals
    return offset
  end,
  ["A"] = function(tree, buf, offset)
    offset = add_compId(tree, buf, offset)
    return offset
  end,
  
  -- fileRename 
  ["b"] = function (tree, buf, offset)
    offset = add_string(tree, f_soxStr, buf, offset)
    offset = add_string(tree, f_soxStr, buf, offset)
    return offset
  end,
  
  -- readComp 
  ["c"] = function (tree, buf, offset)
    offset = add_compId(tree, buf, offset)

    local whatElem = tree:add(f_compWhat, buf(offset, 1))
    local whatChar = buf(offset, 1):string()
    local mapping = {t = "tree", c = "config", r = "runtime", l = "links"}
    whatElem:append_text("(" .. mapping[whatChar] .. ")")
    offset = offset + 1

    return offset
  end,
  ["C"] = function (tree, buf, offset)
    offset = add_compId(tree, buf, offset)
    
    local compDataChar = buf(offset, 1):string()
    if compDataChar == 't' then 
      offset = parse_comp_tree(tree, buf, offset)
    elseif compDataChar == 'l' then 
      offset = parse_comp_links(tree, buf, offset)
    elseif compDataChar == 'c' or compDataChar == 'r' or compDataChar == 'C' or compDataChar == 'R' then 
      offset = parse_comp_props(tree, buf, offset)
    end
    return offset
  end,
  
  -- delete
  ["d"] = function (tree, buf, offset)
    offset = add_compId(tree, buf, offset)
    return offset
  end,
  
  -- event
  ["e"] = function (tree, buf, offset)
    offset = add_compId(tree, buf, offset)

    local compDataChar = buf(offset, 1):string()
    if compDataChar == 't' then 
      offset = parse_comp_tree(tree, buf, offset)
    elseif compDataChar == 'l' then 
      offset = parse_comp_links(tree, buf, offset)
    elseif compDataChar == 'c' or compDataChar == 'r' or compDataChar == 'C' or compDataChar == 'R' then 
      offset = parse_comp_props(tree, buf, offset)
    end
    return offset
  end,
  
  -- fileOpen 
  ["f"] = function (tree, buf, offset)
    tree:add(f_fileOpenMethod, buf(offset, 1)) 
    offset = offset + 1
    
    offset = add_string(tree, f_fileUri, buf, offset)
    
    tree:add(f_fileSize, buf(offset, 4))
    offset = offset + 4
    
    tree:add(buf(offset, 2), "suggestedChunkSize", buf(offset, 2):uint())
    offset = offset + 2
    
    offset = add_file_headers(tree, buf, offset)

    return offset
  end,
  ["F"] = function (tree, buf, offset)
    tree:add(f_fileSize, buf(offset, 4))
    offset = offset + 4
    
    tree:add(buf(offset, 2), "actualChunkSize", buf(offset, 2):uint())
    offset = offset + 2
    
    offset = add_file_headers(tree, buf, offset)

    return offset
  end,

  -- invoke
  ["i"] = function (tree, buf, offset)
    offset = add_compId(tree, buf, offset)
    
    tree:add(f_soxSlotId, buf(offset, 1))
    offset = offset + 1
    
    -- TODO: parse val
    return offset
  end,
  
  -- fileChunk
  ["k"] = function (tree, buf, offset)
    tree:add(f_chunkNum, buf(offset, 2))
    offset = offset + 2
    
    local chunkSize = buf(offset, 2):uint()
    tree:add(f_chunkSize, buf(offset, 2))
    offset = offset + 2
    
    tree:add(f_soxBytes, buf(offset, chunkSize))
    offset = offset + chunkSize

    return offset
  end,
  
  -- link
  ["l"] = function (tree, buf, offset)
    local linkType = buf(offset, 1)
    local linkTypeElem = tree:add(f_linkAction, linkType)
    offset = offset + 1
    
    local fromCompId = buf(offset, 2):uint()
    tree:add(f_linkFromCompId, buf(offset, 2))
    offset = offset + 2
    local fromSlotId = buf(offset, 1):uint()
    tree:add(f_linkFromSlotId, buf(offset, 1))
    offset = offset + 1

    local toCompId = buf(offset, 2):uint()
    tree:add(f_linkToCompId, buf(offset, 2))
    offset = offset + 2
    local toSlotId = buf(offset, 1):uint()
    tree:add(f_linkToSlotId, buf(offset, 1))
    offset = offset + 1

    local label = "" .. fromCompId .. "." .. fromSlotId .. " -> " .. toCompId .. "." .. toSlotId
    if string.char(linkType:uint()) == 'a' then
      linkTypeElem:append_text("(add link: " .. label .. ")")
    else 
      linkTypeElem:append_text("(delete link: " .. label .. ")")
    end
    return offset
  end,
  
  -- rename
  ["n"] = function (tree, buf, offset)
    offset = add_compId(tree, buf, offset)
    offset = add_string(tree, f_compName, buf, offset)
  
    return offset
  end,
  
  -- reorder
  ["o"] = function (tree, buf, offset)
    offset = add_compId(tree, buf, offset)

    local numChildren = buf(offset, 1):uint()
    tree:add(buf(offset, 1), "numChildren", buf(offset, 1):uint())
    offset = offset + 1
    
    offset = add_id_list(numChildren, tree, buf, offset)

    return offset
  end,
  
  -- readProp
  ["r"] = function (tree, buf, offset)
    offset = add_compId(tree, buf, offset)

    tree:add(buf(offset, 1), "propId", buf(offset, 1):uint())
    offset = offset + 1

    return offset
  end,
  ["R"] = function (tree, buf, offset)
    tree:add(buf(offset, 1), "ignored", buf(offset, 1):uint())
    offset = offset + 1
    -- TODO: parse prop val
    return offset
  end,
  
  -- subscribe
  ["s"] = function (tree, buf, offset)
    offset = add_whatMask(tree, buf, offset)
    
    local num = buf(offset, 1):uint()
    tree:add(buf(offset, 1), "num", num)
    offset = offset + 1
    
    offset = add_id_list(num, tree, buf, offset)

    return offset
  end,
  ["S"] = function (tree, buf, offset)
    tree:add(buf(offset, 1), "numSubscribed", buf(offset, 1):uint())
    offset = offset + 1
    return offset
  end,
  
  -- unsubscribe
  ["u"] = function (tree, buf, offset)
    offset = add_whatMask(tree, buf, offset)

    local num = buf(offset, 1):uint()
    tree:add(buf(offset, 1), "num", num)
    offset = offset + 1

    offset = add_id_list(num, tree, buf, offset)

    return offset
  end,
  
  -- version
  ["V"] = function (tree, buf, offset)
    local kitCount = buf(offset, 1):uint()
    tree:add(buf(offset, 1), "kitCount", kitCount)
    offset = offset + 1
    
    local parts = {}
    local start = offset
    for i=1, kitCount do 
      local str = buf(offset):stringz("utf-8")
      offset = offset + string.len(str) + 1
      table.insert(parts, str .. "@" .. string.format("0x%x", buf(offset, 4):uint()))
      offset = offset + 4
    end
    tree:add(buf(start, offset-start), "kits", table.concat(parts, ", "))

    return offset
  end,
  
  -- write
  ["w"] = function (tree, buf, offset)
    offset = add_compId(tree, buf, offset)

    tree:add(f_soxSlotId, buf(offset, 1))
    offset = offset + 1

    --TODO: handle val
    
    return offset
  end,
  
  -- versionMore
  ["Y"] = function (tree, buf, offset)
    offset = add_string(tree, f_soxPlatformId, buf, offset)
    
    local scodeFlags = buf(offset, 1):uint()
    tree:add(buf(offset, 1), "scodeFlags", string.format("0x%02x", scodeFlags))
    offset = offset + 1
    
    -- TODO: very hard to parse versionMore data
    -- local start = offset
    -- local versions = {}
    -- local str = buf(offset):stringz("utf-8")
    -- table.insert(versions, str)
    -- offset = offset + string.len(str) + 1
    
    return offset
  end,
  
  -- query
  ["q"] = function (tree, buf, offset)
    tree:add(buf(offset, 1), "queryType", buf(offset, 1):string())
    offset = offset + 1
    
    while offset < buf:len() do 
      tree:add(f_kitId, buf(offset, 1))
      offset = offset + 1

      tree:add(f_typeId, buf(offset, 1))
      offset = offset + 1
    end
    return offset
  end,
  ["Q"] = function (tree, buf, offset)
    local ids = {}
    local start = offset
    local compId = buf(offset, 2):uint()
    while compId ~= 0xffff do 
      table.insert(ids, compId) 
      offset = offset + 2

      compId = buf(offset, 2):uint()
    end
    offset = offset + 2
    
    tree:add(buf(start, offset-start), "compIds", table.concat(ids, ","))

    return offset
  end,
  
  -- error
  ["!"] = function (tree, buf, offset)
    offset = add_string(tree, f_soxError, buf, offset)
    return offset
  end,

};

-- helpers 

function parse_sox(tree, buf, offset)
    local cmdVal = buf(offset, 1):uint()
    local cmdElem = tree:add(f_soxCmd, buf(offset, 1))
    if sox_cmds[string.char(cmdVal)] ~= nil then
      cmdElem:append_text(" ('" .. string.char(cmdVal) .. "' " .. sox_cmds[string.char(cmdVal)] .. ")")
    elseif sox_cmds[string.char(cmdVal):lower()] ~= nil then
      cmdElem:append_text(" ('" .. string.char(cmdVal) .. "' " ..  sox_cmds[string.char(cmdVal):lower()] .. " response)")
    end
    offset = offset + 1

    tree:add(f_soxReplyNum, buf(offset, 1))
    offset = offset + 1
    
    local cmdChar = string.char(cmdVal)
    local handler = sox_handlers[cmdChar]
    if handler ~= nil then
      offset = handler(tree, buf, offset)
    end
    
    -- output all pending bytes
    if offset < buf:len() then 
      tree:add(f_soxBytes, buf(offset))
      offset = buf:len()
    end

    return offset
end

function add_header(tree, buf, offset)
  local headerVal = buf(offset, 1)
  local headerIdVal = headerVal:bitfield(0, 6)
  local headerTypeVal = headerVal:bitfield(6, 2)
  
  local headerName = ''
  if header_mappings[headerVal:uint()] ~= nil then
    headerName = header_mappings[headerVal:uint()]
  end

  if headerTypeVal == 0 then 
    local elem = tree:add(buf(offset, 1), headerName)
    offset = offset + 1

    if headerIdVal == 0x0d then
      elem:append_text(" (" .. error_codes[headerVal:uint()] .. ")")
    end
  elseif headerTypeVal == 1 then 
    tree:add(buf(offset, 3), headerName, buf(offset+1, 2):uint())
    offset = offset + 3
  elseif headerTypeVal == 2 then
    local str = buf(offset+1):stringz("utf-8")
    tree:add(buf(offset, string.len(str) + 1 + 1), headerName, str)
    offset = offset + string.len(str) + 1 + 1
  elseif headerTypeVal == 3 then 
    local len = buf(offset+1, 1):uint()
    local ba = buf(offset+2, len):bytes()
    local baStr = ''
    for i=0, ba:len()-1 do 
      baStr = baStr .. string.format("%02x", ba:get_index(i))
    end

    tree:add(buf(offset, len+2), headerName, baStr)
    offset = offset + len + 2
  end

  return offset
end

function parse_headers(subtree, buf, headerOffset, headerNum)
  for i=1, headerNum do 
    headerOffset = add_header(subtree, buf, headerOffset)
  end

  return headerOffset 
end

-- dissector function 
function dasp_proto.dissector(buf, pinfo, tree)
  if buf:len() < 5 then return end

  pinfo.cols.protocol = dasp_proto.name

  local subtree = tree:add(dasp_proto, buf(0))
  subtree:add(f_sessionId, buf(0, 2))
  subtree:add(f_seqNum, buf(2, 2))

  local typeVal = buf(4, 1):bitfield(0, 4)
  local msgType = subtree:add(f_msgType, buf(4, 1), typeVal)
  if msg_types[typeVal] ~= nil then
    msgType:append_text(" (" .. msg_types[typeVal][1] .. ")")
  end

  local headerNum = buf(4, 1):bitfield(4, 4)
  subtree:add(f_headerFieldNum, buf(4, 1), headerNum)
  
  -- header fields
  local headerOffset = 5
  headerOffset = parse_headers(subtree, buf, headerOffset, headerNum)

  if typeVal == 0x06 and headerOffset < buf:len() then 
    local soxtree = subtree:add(sox_proto, buf(headerOffset))
    parse_sox(soxtree, buf, headerOffset)
  end
end

function dasp_proto.init()
  local udp_table = DissectorTable.get("udp.port")
  udp_table:add(dasp_proto.prefs["udp_port"], dasp_proto)
end
