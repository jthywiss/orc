{- MultiAlarm.orc
 - 
 - $Id$
 - 
 - Created by misra on Apr 5, 2010 10:41:50 AM
 -}

{- 
This class defines a multiple Alarm clock in which several alarms 
can be set simultaneously. The alarms can also be cancelled.
For the instance m of Multialarm(),

  m.set(id,t), where id is a string and t an integer
    returns a signal after t time units, unless it is cancelled.

  m.cancel(id) cancels the alarm set for id and emits a signal; 
   if no alarm has been set for id, the call is treated as skip, and
   a signal is immediately returned.

Implementation Strategy:
The inner class Alarm controls the setting and possible
cancellation of a single alarm. For the instance p of Alarm(),

  p.set(t), where t is an integer, 
    returns a signal after t time units, unless it is cancelled.

  p.cancel() cancels the alarm, if it is invoked before t, so that
   p.set(t) never responds; this function always returns a signal.

Multialarm() creates a  new instance of Alarm() for every alarm that
is set. It stores the id of the alarm and the address (name) 
of the instance in a global data structure, a map, as a (key,value)
pair. For cancellation, it removes the value associated with this id;
if it is null, it emits a signal immediately; otherwise, it calls
the cancel procedure of the associated instance of Alarm().
-}

def class Multialarm() =

  def class Alarm() =
    val run = Ref(true)

    def set(t) = Rwait(t) >> If(run?)
    def cancel() = run := false
  stop

 class Map = java.util.HashMap

 val alarmlist = Map()

 def set(id,t) = 
  val a = Alarm()
  alarmlist.put(id,a) >> a.set(t)

 def cancel(id) =
  alarmlist.remove(id) >b> 
   (if b = null then signal else b.cancel())

stop

val m = Multialarm()

  m.set("first", 500) >> "first alarm" 
| m.set("second", 100) >> "second alarm" 
| Rwait(400) >> m.cancel("first") >> "first cancelled"
| m.cancel("third") >> "No third alarm has been set"

{-
OUTPUT:
"No third alarm has been set"
"second alarm"
"first cancelled"
-}
