{- BoundedChannelTest.orc -- Orc program to test BoundedChannel
 - 
 - $Id$
 - 
 - Split from BoundedChannel.orc, Created by misra on Mar 11, 2010 8:57:43 PM
 -}

include "BoundedChannel.inc"

val c = BBuffer(2)
  c.put(5) >> c.put(2) >> c.put(50) >> "yes"
| Rtimer(2000) >> c.get()
| Rtimer(4000) >> c.get() >> c.get()

{-
OUTPUT:
5
"yes"
50
-}
