{- site1.orc -- Test a simple site call
 -
 - $Id$
 -
 - Created by amp on Nov 30, 2014 10:57:40 PM
 -}

site Test(x :: Integer, y :: Integer) :: Integer = (x + 1) / y

Test(11, 2)

{-
OUTPUT:
6
-}