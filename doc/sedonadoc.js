//  Copyright (c) 2009, Tridium, Inc.
//  Licensed under the Academic Free License version 3.0
//

function copyright() {
  var START_YEAR = 2009;
  var year = new Date().getFullYear();
  if (year == START_YEAR) {
    document.write("Copyright &#169; " + START_YEAR + ", Tridium, Inc.");
  } else {
    document.write("Copyright &#169; " + START_YEAR + "-" + year + ", Tridium, Inc.");
  }
}