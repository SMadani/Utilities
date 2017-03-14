open System
Console.WriteLine("Please enter the sequence of keys. Special keys should be surrounded by {}.")
let command = Console.ReadLine()
Console.WriteLine("Please enter the number of milliseconds you'd like to delay.")
let sleepTime = int (Console.ReadLine())
Console.WriteLine("Press Enter to start the countdown...")
Console.ReadLine()
Threading.Thread.Sleep(sleepTime)
Windows.Forms.SendKeys.SendWait(command)