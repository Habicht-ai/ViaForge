Option Explicit
Dim shell, fs, root, quote
Set shell = CreateObject("WScript.Shell")
Set fs = CreateObject("Scripting.FileSystemObject")
root = fs.GetParentFolderName(WScript.ScriptFullName)
quote = Chr(34)
shell.CurrentDirectory = root
shell.Run "pyw -3 " & quote & root & "\tools\test-servers\webapp.py" & quote & " --open", 0, False
