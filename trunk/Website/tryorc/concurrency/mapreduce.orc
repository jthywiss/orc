{-
A demonstration of the map/reduce idiom expressed in Orc.
This program is not actually allocating the map/reduce
tasks to different machines, but it shows how cleanly the
high-level data flow can be expressed in Orc; all that's
needed to actually distribute the computation is suitable
sites to which the data can be passed at each phase.
-}

{-
The first half of the program is the map/reduce framework,
which is assumed fixed.
-}
val table = Buffer()
-- Given a key, return a function which stores
-- that key.  Here we store all keys in a single
-- buffer, but a real implementation would hash
-- keys to a buffer on the machine where they would
-- be reduced
def partition(_)(k,v) = table.put((k,v))

-- To simulate reading data we publish each element
def read(data) = each(data)
-- To simulate writing data we just publish it
def write(datum) = datum

-- Implement a retry policy; if the site f
-- doesn't respond in 1 second, call it again
def retry(f)(a,b) =
  val (ok, value) = (true, f(a,b)) | (false, Rtimer(1000))
  if ok then value else retry(f)

-- Compare two tuples by their first element
def lt((k1,_), (k2,_)) = (k1 < k2)
def eq(x,y) = (x = y)

-- The map phase reads data, maps it,
-- partitions it, and stores it
def MAP(mapper, data) =
  val rmapper = retry(mapper)
  read(data) >(k1,v1)>
  rmapper(k1,v1) >kvs>
  each(kvs) >(k2,v2)>
  partition(k2) >store>
  store(k2,v2)

-- The reduce phase sorts data,
-- groups it, reduces it, and writes it
def REDUCE(reducer, table) =
  sortBy(lt, table.getAll()) >data>
  each(groupBy(eq, data)) >(k,vs)>
  reducer(k, vs) >v>
  write((k,v))

{-
The second half of the program is the user-provided
mapper and reducer functions. For this example,
our data will be lists of numbers, and we will
count the total number of times each number appears.
-}

def mapper(name, numbers) =
  def makeTuple(v) = (v,1)
  map(makeTuple, numbers)

def reducer(number, counts) =
  foldl(lambda (a,b) = a + b, counts, 0)

val data = [
  ("primes", [2, 3, 5, 7, 11]),
  ("odd", [1, 3, 5, 7, 9, 11]) ]

MAP(mapper, data) >> stop
; REDUCE(reducer, table)
