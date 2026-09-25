"""Run edge_probe unchanged, adding Java 8 pause logs and a passive host clock trace.

Arguments are forwarded to edge_probe (ViaForge only), except --jfr/--stacks.
No delay is injected. --jfr adds the runtime's passive Flight Recorder profiler.
Clock samples stay in memory until completion; JVM diagnostics have their own
file and do not touch the movement/packet observers or test acceptance rules.
"""
import hashlib
import json
from pathlib import Path
import runpy
import subprocess
import sys
import threading
import time
import zipfile


def main():
    record_jfr = '--jfr' in sys.argv
    if record_jfr: sys.argv.remove('--jfr')
    record_stacks = '--stacks' in sys.argv
    if record_stacks: sys.argv.remove('--stacks')
    import boat_probe
    boat_probe.assert_no_client()
    if '--client' in sys.argv and sys.argv[sys.argv.index('--client') + 1] != 'viaforge':
        raise SystemExit('Pause logging is for the ViaForge Java 8 runtime only')
    root = Path(__file__).resolve().parents[2]
    folder = root / 'build/inspection/flight-edges' / ('stall-' + str(time.time_ns()))
    folder.mkdir(parents=True)
    (folder / 'edge_stall_probe.py').write_bytes(Path(__file__).read_bytes())
    script = folder / 'client-pause-log.gradle'
    gc_log = (folder / 'client-gc.log').as_posix().replace("'", "\\'")
    extra = ("    jvmArgs '-XX:StartFlightRecording=filename=" +
             (folder / 'client.jfr').as_posix().replace("'", "\\'") +
             ",settings=profile,dumponexit=true,maxsize=128M'\n") if record_jfr else ''
    if record_stacks:
        import lab
        source = Path(__file__).parent / 'native-probe/ThreadStallProbe.java'
        (folder / source.name).write_bytes(source.read_bytes())
        classes = folder / 'classes'
        classes.mkdir()
        javac = Path(lab.java({'java': 21})).with_name('javac.exe')
        subprocess.run([str(javac), '--release', '8', '-d', str(classes), str(source)],
                       check=True, creationflags=lab.NO_WINDOW)
        agent = folder / 'ThreadStallProbe.jar'
        with zipfile.ZipFile(agent, 'w', zipfile.ZIP_DEFLATED) as archive:
            archive.writestr('META-INF/MANIFEST.MF',
                'Manifest-Version: 1.0\nPremain-Class: viaforge.lab.ThreadStallProbe\n\n')
            for file in classes.rglob('*.class'): archive.write(file, file.relative_to(classes).as_posix())
        argument = '-javaagent:' + agent.as_posix() + '=' + (folder / 'stacks.tsv').as_posix()
        extra += "    jvmArgs '" + argument.replace("'", "\\'") + "'\n"
    script.write_text("gradle.projectsEvaluated {\n"
        "  rootProject.tasks.named('runClient').configure {\n"
        "    jvmArgs '-XX:+PrintGCApplicationStoppedTime', '-XX:+PrintGCDateStamps',\n"
        "            '-XX:+PrintGCDetails', '-Xloggc:" + gc_log + "'\n"
        + extra + "  }\n}\n", encoding='utf-8')
    samples = []
    stop = threading.Event()

    def observe_clock():
        while not stop.is_set():
            samples.append((time.time_ns(), time.perf_counter_ns()))
            stop.wait(.025)

    original_popen = subprocess.Popen

    def launch(args, *positional, **kwargs):
        if isinstance(args, list) and any(str(a).endswith('build.bat') for a in args) and 'runClient' in args:
            args = [*args, '--init-script', str(script)]
        return original_popen(args, *positional, **kwargs)

    print('PASSIVE STALL DIAGNOSTICS ' + str(folder), flush=True)
    thread = threading.Thread(target=observe_clock, daemon=True)
    thread.start()
    subprocess.Popen = launch
    try:
        runpy.run_path(str(Path(__file__).with_name('edge_probe.py')), run_name='__main__')
    finally:
        subprocess.Popen = original_popen
        stop.set()
        thread.join()
        (folder / 'clock.csv').write_text('utc_ns,monotonic_ns\n' +
            ''.join(f'{wall},{mono}\n' for wall, mono in samples), encoding='ascii')
        files = [folder / 'edge_stall_probe.py', script, folder / 'clock.csv']
        if (folder / 'client-gc.log').exists(): files.append(folder / 'client-gc.log')
        if (folder / 'client.jfr').exists(): files.append(folder / 'client.jfr')
        for filename in ('ThreadStallProbe.java', 'ThreadStallProbe.jar', 'stacks.tsv'):
            if (folder / filename).exists(): files.append(folder / filename)
        (folder / 'evidence.json').write_text(json.dumps(dict(
            arguments=sys.argv[1:], jfr=record_jfr, stacks=record_stacks, delay_injected=False, clock_samples=len(samples),
            files={p.relative_to(root).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest() for p in files}),
            indent=2) + '\n', encoding='utf-8')


if __name__ == '__main__':
    main()
