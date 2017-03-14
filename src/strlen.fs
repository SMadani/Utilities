[<EntryPoint>]
let main(args) =
    printfn "%i" (String.length (Array.get args 0))
    0