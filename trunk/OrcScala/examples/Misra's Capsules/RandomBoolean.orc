{- RandomBoolean.orc
 - 
 - $Id$
 - 
 - Created by misra on Mar 13, 2010 3:43:38 PM
 -}

{- RandomBool() has:
   f a real number between 0 and 1,
   t an integer,
   A call to site RandomBool(f,t) responds with 
     probability f
     afer some random time at most t
     with a random boolean.
   
  It has just one method, main(). Ithandles one call at a time.
-}

def capsule RandomBool() =
  val s = Semaphore(1) -- to allow only one call to execute.
  def main(f,t) =  
   IfT(urandom() <: f) >>  s.acquire() >>
   random(t) >w> Rtimer(w) >>
   (random(2) = 1) >v>
   s.release() >> 
   (w,v)

stop

val rb = RandomBool().main
val (_,x) = rb(0.5,3000) 
val (_,y) = rb(1,3000)
val z = IfT(x) >> true | IfT(y) >> true | x||y
 z
