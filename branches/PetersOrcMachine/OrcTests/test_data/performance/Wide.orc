{- Wide.orc -- Orc program Wide
 - 
 - $Id$
 - 
 - Created by amp on Jul 15, 2013 4:57:57 PM
 -}

include "timeIt.inc"

val n = 40000
val c = Counter(n)

timeIt(lambda () =
  upto(n) >> c.dec() >> stop |
  c.onZero() >> "Done!"
  ) 

{-
BENCHMARK
-}

