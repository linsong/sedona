//
// Copyright (c) 2007 Tridium, Inc.
// Licensed under the Academic Free License version 3.0
//
// History:
//   26 Apr 07  Brian Frank  Creation
//

#include "sedona.h"
#include "float.h"

//////////////////////////////////////////////////////////////////////////
// Error Handling
//////////////////////////////////////////////////////////////////////////

static Cell accessError(SedonaVM* vm, const char* method, void* comp, void* slot)
{
#ifdef SCODE_DEBUG
  void* type = getCompType(vm, comp);
  void* kit  = getTypeKit(vm, type);
  const char* kitName  = getKitName(vm, kit);
  const char* typeName = getTypeName(vm, type);
  const char* slotName = getSlotName(vm, slot);

  printf("ERROR: %s::%s.%s %s\n", kitName, typeName, slotName, method);
#endif

  return zeroCell;
}

//////////////////////////////////////////////////////////////////////////
// Getters
//////////////////////////////////////////////////////////////////////////

// bool Component.getBool(Slot)
Cell sys_Component_getBool(SedonaVM* vm, Cell* params)
{
  uint8_t* self   = params[0].aval;
  void* slot      = params[1].aval;
  uint16_t typeId = getTypeId(vm, getSlotType(vm, slot));
  uint16_t offset = getSlotHandle(vm, slot);
  Cell ret;

  if (typeId != BoolTypeId)
    return accessError(vm, "getBool", self, slot);

  ret.ival = getByte(self, offset);
  return ret;
}

// int Component.getInt(Slot)
Cell sys_Component_getInt(SedonaVM* vm, Cell* params)
{
  uint8_t* self   = params[0].aval;
  uint8_t* slot   = params[1].aval;
  uint16_t typeId = getTypeId(vm, getSlotType(vm, slot));
  uint16_t offset = getSlotHandle(vm, slot);
  Cell ret;

  switch (typeId)
  {
    case ByteTypeId:
      ret.ival = getByte(self, offset);
      break;
    case ShortTypeId:
      ret.ival = getShort(self, offset);
      break;
    case IntTypeId:
      ret.ival = getInt(self, offset);
      break;
    default:
      return accessError(vm, "getInt", self, slot);
  }

  return ret;
}

// long Component.getLong(Slot)
int64_t sys_Component_getLong(SedonaVM* vm, Cell* params)
{
  uint8_t* self   = params[0].aval;
  uint8_t* slot   = params[1].aval;
  uint16_t typeId = getTypeId(vm, getSlotType(vm, slot));
  uint16_t offset = getSlotHandle(vm, slot);

  if (typeId != LongTypeId)                
  {
    accessError(vm, "getLong", self, slot);
    return 0;
  }

  return getWide(self, offset);
}

// int Component.getFloat(Slot)
Cell sys_Component_getFloat(SedonaVM* vm, Cell* params)
{
  uint8_t* self   = params[0].aval;
  uint8_t* slot   = params[1].aval;
  uint16_t typeId = getTypeId(vm, getSlotType(vm, slot));
  uint16_t offset = getSlotHandle(vm, slot);
  Cell ret;

  if (typeId != FloatTypeId)
    return accessError(vm, "getFloat", self, slot);

  // get as int, to avoid NaN weirdnesses on some platforms
  ret.ival = getInt(self, offset);
  return ret;
}

// long Component.getDouble(Slot)
int64_t sys_Component_getDouble(SedonaVM* vm, Cell* params)
{
  uint8_t* self   = params[0].aval;
  uint8_t* slot   = params[1].aval;
  uint16_t typeId = getTypeId(vm, getSlotType(vm, slot));
  uint16_t offset = getSlotHandle(vm, slot);

  if (typeId != DoubleTypeId)           
  {
    accessError(vm, "getDouble", self, slot);
    return 0;
  }

  return getWide(self, offset);
}

// Str Component.getBuf(Slot)
Cell sys_Component_getBuf(SedonaVM* vm, Cell* params)
{
  uint8_t* self   = params[0].aval;
  uint8_t* slot   = params[1].aval;
  uint16_t typeId = getTypeId(vm, getSlotType(vm, slot));
  uint16_t offset = getSlotHandle(vm, slot);
  Cell ret;

  if (typeId != BufTypeId)
    return accessError(vm, "getBuf", self, slot);

  ret.aval = getInline(self, offset);
  return ret;
}

//////////////////////////////////////////////////////////////////////////
// Setters
//////////////////////////////////////////////////////////////////////////

// bool Component.setBool(Slot, bool)
Cell sys_Component_doSetBool(SedonaVM* vm, Cell* params)
{
  uint8_t* self   = params[0].aval;
  void* slot      = params[1].aval;
  uint8_t val     = params[2].ival;
  uint16_t typeId = getTypeId(vm, getSlotType(vm, slot));
  uint16_t offset = getSlotHandle(vm, slot);

  // type check
  if (typeId != BoolTypeId)
    return accessError(vm, "setBool", self, slot);

  // short circuit if no change
  if (getByte(self, offset) == val)
    return falseCell;
  
  // update memory location  
  setByte(self, offset, val);
  return trueCell;
}

// bool Component.doSetInt(Slot, int)
Cell sys_Component_doSetInt(SedonaVM* vm, Cell* params)
{
  uint8_t* self   = params[0].aval;
  uint8_t* slot   = params[1].aval;
  int32_t val     = params[2].ival;
  uint16_t typeId = getTypeId(vm, getSlotType(vm, slot));
  uint16_t offset = getSlotHandle(vm, slot);

  switch (typeId)
  {
    case ByteTypeId:          
      if (getByte(self, offset) == val) return falseCell;
      setByte(self, offset, val);
      break;
    case ShortTypeId:
      if (getShort(self, offset) == val) return falseCell;
      setShort(self, offset, val);
      break;
    case IntTypeId:
      if (getInt(self, offset) == val) return falseCell;
      setInt(self, offset, val);
      break;
    default:
      return accessError(vm, "setInt", self, slot);
  }

  return trueCell;
}

// bool Component.doSetLong(Slot, long)
Cell sys_Component_doSetLong(SedonaVM* vm, Cell* params)
{
  uint8_t* self   = params[0].aval;
  uint8_t* slot   = params[1].aval;
  int64_t val     = *(int64_t*)(params+2);
  uint16_t typeId = getTypeId(vm, getSlotType(vm, slot));
  uint16_t offset = getSlotHandle(vm, slot);
  
  // type check
  if (typeId != LongTypeId)
    return accessError(vm, "setLong", self, slot);

  // short circuit if no change
  if (getWide(self, offset) == val)
    return falseCell;

  // update memory location
  setWide(self, offset, val);
  return trueCell;
}


// bool Component.doSetFloat(Slot, float)
Cell sys_Component_doSetFloat(SedonaVM* vm, Cell* params)
{
  uint8_t* self   = params[0].aval;
  uint8_t* slot   = params[1].aval;
  Cell newval     = params[2];
  uint16_t typeId = getTypeId(vm, getSlotType(vm, slot));
  uint16_t offset = getSlotHandle(vm, slot);
  
  Cell oldval;

  // type check
  if (typeId != FloatTypeId)
    return accessError(vm, "setFloat", self, slot);

  // get value as int, to avoid NaN weirdnesses on some platforms
  oldval.ival = getInt(self, offset);

  // short circuit if no change - compare bits so NaN==NaN (Sedona spec)
  if (oldval.ival == newval.ival)
    return falseCell;

  // update memory location
  setFloat(self, offset, newval.fval);
  return trueCell;
}


// bool Component.doSetDouble(Slot, double)
Cell sys_Component_doSetDouble(SedonaVM* vm, Cell* params)
{
  uint8_t* self   = params[0].aval;
  uint8_t* slot   = params[1].aval;
  int64_t val     = *(int64_t*)(params+2);
  uint16_t typeId = getTypeId(vm, getSlotType(vm, slot));
  uint16_t offset = getSlotHandle(vm, slot);
  
  // type check
  if (typeId != DoubleTypeId)
    return accessError(vm, "setDouble", self, slot);

  if (getWide(self, offset) == val)
    return falseCell;

  // update memory location
  setWide(self, offset, val);
  return trueCell;
}


//////////////////////////////////////////////////////////////////////////
// Invokes
//////////////////////////////////////////////////////////////////////////

/**
 * This function will return the method offset for an action.  All actions are
 * by definition virtual. When you obtain the slot handle of the slot parameter
 * for an action, the handle contains the vtable index for the action method -
 * not the method offset.
 *
 * Given the code base, the instance object, and the vtable index, we can 
 * lookup the method implementation for the action.  Typical usage in an
 * invoke<Type> action method might be:
 *
 *  uint16_t methodOffset = 
 *    getActionMethod(vm->codeBaseAddr, params[0].aval, getSlotType(vm, params[1].aval)); 
 */
uint16_t getActionMethod(const uint8_t* cb, const uint8_t* self, const uint16_t vidx)
{
  return ((uint16_t*)block2addr(cb, *(uint16_t*)self))[vidx];
}

// void Component.invokeVoid(Slot)
Cell sys_Component_invokeVoid(SedonaVM* vm, Cell* params)
{
  uint8_t* self   = params[0].aval;
  void* slot      = params[1].aval;
  int typeId      = getTypeId(vm, getSlotType(vm, slot));
  uint16_t vidx   = getSlotHandle(vm, slot);
  Cell args[1];

  if (typeId != VoidTypeId)
    return accessError(vm, "invokeVoid", self, slot);

  args[0].aval = self;
  vm->call(vm, getActionMethod(vm->codeBaseAddr, self, vidx), args, 1);

  return nullCell;
}

// void Component.invokeBool(Slot, bool)
Cell sys_Component_invokeBool(SedonaVM* vm, Cell* params)
{
  uint8_t* self   = params[0].aval;
  void* slot      = params[1].aval;
  uint8_t val     = params[2].ival;
  int typeId      = getTypeId(vm, getSlotType(vm, slot));
  uint16_t vidx   = getSlotHandle(vm, slot);
  Cell args[2];

  if (typeId != BoolTypeId)
    return accessError(vm, "invokeBool", self, slot);

  args[0].aval = self;
  args[1].ival = val;
  vm->call(vm, getActionMethod(vm->codeBaseAddr, self, vidx), args, 2);

  return nullCell;
}

// void Component.invokeInt(Slot, int)
Cell sys_Component_invokeInt(SedonaVM* vm, Cell* params)
{
  uint8_t* self   = params[0].aval;
  void* slot      = params[1].aval;
  int32_t val     = params[2].ival;
  int typeId      = getTypeId(vm, getSlotType(vm, slot));
  uint16_t vidx   = getSlotHandle(vm, slot);
  Cell args[2];

  if (typeId != IntTypeId)
    return accessError(vm, "invokeInt", self, slot);

  args[0].aval = self;
  args[1].ival = val;
  vm->call(vm, getActionMethod(vm->codeBaseAddr, self, vidx), args, 2);

  return nullCell;
}

// void Component.invokeLong(Slot, long)
Cell sys_Component_invokeLong(SedonaVM* vm, Cell* params)
{
  uint8_t* self   = params[0].aval;
  void* slot      = params[1].aval;
  int64_t val     = *(int64_t*)(params+2);
  int typeId      = getTypeId(vm, getSlotType(vm, slot));
  uint16_t vidx   = getSlotHandle(vm, slot);
  Cell args[3];

  if (typeId != LongTypeId)
    return accessError(vm, "invokeLong", self, slot);

  args[0].aval = self;
  *(int64_t*)(args+1) = val;
  vm->call(vm, getActionMethod(vm->codeBaseAddr, self, vidx), args, 3);

  return nullCell;
} 

// void Component.invokeFloat(Slot, float)
Cell sys_Component_invokeFloat(SedonaVM* vm, Cell* params)
{
  uint8_t* self   = params[0].aval;
  void* slot      = params[1].aval;
  float val       = params[2].fval;
  int typeId      = getTypeId(vm, getSlotType(vm, slot));
  uint16_t vidx   = getSlotHandle(vm, slot);
  Cell args[2];

  if (typeId != FloatTypeId)
    return accessError(vm, "invokeFloat", self, slot);

  args[0].aval = self;
  args[1].fval = val;
  vm->call(vm, getActionMethod(vm->codeBaseAddr, self, vidx), args, 2);

  return nullCell;
} 

// void Component.invokeDouble(Slot, double)
Cell sys_Component_invokeDouble(SedonaVM* vm, Cell* params)
{
  uint8_t* self   = params[0].aval;
  void* slot      = params[1].aval;
  int64_t val     = *(int64_t*)(params+2);
  int typeId      = getTypeId(vm, getSlotType(vm, slot));
  uint16_t vidx   = getSlotHandle(vm, slot);
  Cell args[3];

  if (typeId != DoubleTypeId)
    return accessError(vm, "invokeDouble", self, slot);

  args[0].aval = self;
  *(int64_t*)(args+1) = val;
  vm->call(vm, getActionMethod(vm->codeBaseAddr, self, vidx), args, 3);

  return nullCell;
} 

// void Component.invokeBuf(Slot, Buf)
Cell sys_Component_invokeBuf(SedonaVM* vm, Cell* params)
{
  uint8_t* self   = params[0].aval;
  void* slot      = params[1].aval;
  uint8_t* val    = params[2].aval;
  int typeId      = getTypeId(vm, getSlotType(vm, slot));
  uint16_t vidx   = getSlotHandle(vm, slot);
  Cell args[2];

  if (typeId != BufTypeId)
    return accessError(vm, "invokeBuf", self, slot);

  args[0].aval = self;
  args[1].aval = val;
  vm->call(vm, getActionMethod(vm->codeBaseAddr, self, vidx), args, 2);

  return nullCell;
}


