open System
open System.IO

let isDirectory path = File.GetAttributes(path) = FileAttributes.Directory
let parseInt str = Int32.Parse(str)
let renameFile path newName = File.Move(path, Path.GetDirectoryName(path)+"/"+newName)

let getNewName path mode position arg3 =
    let currentName = Path.GetFileName(path)
    match mode with
                    | "add" ->
                        let addition = arg3
                        match position with
                                    | "before" -> addition+currentName
                                    | "after" -> currentName+addition
                                    | _ -> ""
                    | "remove" ->
                        let nchars = parseInt arg3
                        match position with
                                    | "before" -> currentName.Substring(nchars)
                                    | "after" -> currentName.Substring(0, currentName.Length-nchars)
                                    | _ -> ""
                    | _ -> currentName

[<EntryPoint>]
let main(args) =
    if (Array.length args <> 4) then
        printfn "Usage: renamer [file or directory path] [add/remove] [before/after] [string/number of characters]."
    else
        let path = Array.get args 0
        let mode = Array.get args 1
        let position = Array.get args 2
        let arg3 = Array.get args 3

        if (not <| (Directory.Exists(path) || File.Exists(path))) then
            printfn "Could not access path."
        else
            if (isDirectory path) then
                for file in Directory.EnumerateFiles path do
                    let newName = getNewName file mode position arg3
                    renameFile file newName
            else
                let newName = getNewName path mode position arg3
                renameFile path newName
    0