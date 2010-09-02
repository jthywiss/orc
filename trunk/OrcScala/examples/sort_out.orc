-- amshali
-- Monday, July 05 2010

-- sort output

def sort(input, comparator) =
  val b = Buffer()
  val l = Ref([])
  def sort_aux(x, []) = [x]
  def sort_aux(x, y:[]) = if (comparator(x, y) <: 0) then x:[y] else y:[x]
  def sort_aux(x, y:yl) = if (comparator(x, y) <: 0) then x:y:yl else y:sort_aux(x, yl)
  def sort_buffer() = (b.get() >x> (l := sort_aux(x, l?))  >> sort_buffer() >> stop); signal
 
  signal >> (input() >x> b.put(x)  >> stop; b.close()>>stop) | sort_buffer() >> l?


sort(lambda()=( (1,(2,3)) | (4,true) | (5,[6,7]) | (8,signal) ) >(x,_)> x, 
  lambda(x, y) = x - y)


{-
OUTPUT:
[1, 4, 5, 8]
-}
