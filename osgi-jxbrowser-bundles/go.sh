cd target
../bnd ../swing.bnd
../inst com.teamdev.jxbrowser-swing $1
../bnd ../core.bnd
../inst com.teamdev.jxbrowser $1
../bnd ../mac.bnd
../inst com.teamdev.jxbrowser-mac $1
../bnd ../mac-arm.bnd
../inst com.teamdev.jxbrowser-mac-arm $1
../bnd ../linux.bnd
../inst com.teamdev.jxbrowser-linux64 $1
../bnd ../windows.bnd
../inst com.teamdev.jxbrowser-win64 $1
../bnd ../linux-arm.bnd
../inst com.teamdev.jxbrowser-linux64-arm $1

