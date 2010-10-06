{--
Write a function which accepts a list of sites which
return natural numbers and calls each site in
parallel. If any site returns a value less than 100,
immediately return that value. Otherwise, return the
minimum value returned by any site.
--}

def f(List[lambda() :: Number]) :: Number
def f([]) = error("Non-empty list")
def f(g:[]) = g()
def f(g:rest) =
  val x = g()
  val y = f(rest)
  let(
      If(x <: 100) >> x
    | If(y <: 100) >> y
    | min(x, y)
  )

f([constant(100), constant(50), constant(200), constant(40)])

{-
OUTPUT:
50
-}
