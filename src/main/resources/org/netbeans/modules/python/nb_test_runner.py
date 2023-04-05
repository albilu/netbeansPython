##############################################################################
# Test Runner for NetBeans, based on the unittest TextTestRunner (but prints
# out more information during execution that is parsed and swallowed by the
# NetBeans test runner GUI.)
#
# Tor Norbye <tor@netbeans.org> Dec 20 2008
#
# Updates by @albilu March 18 2023
# Add addExpectedFailure / addUnexpectedSuccess / addSkip / addSubTest methods
# Fixed doctest handling
# Add support for TestLoader().discover to increase the chance to dicover test cases
# Fix single method run
# Easy test path computation
##############################################################################

import inspect
import os
import os.path
import re
import sys
import time
from optparse import OptionParser
from unittest import TestResult


class _NbWritelnDecorator:
    """Used to decorate file-like objects with a handy 'writeln' method"""

    def __init__(self, stream):
        self.stream = stream

    def __getattr__(self, attr):
        return getattr(self.stream, attr)

    def writeln(self, arg=None):
        if arg:
            self.write(arg)
        self.write("\n")  # text-mode streams translate to \r\n if needed


class _NbTextTestResult(TestResult):
    """
    A test result class that can print specially formatted test status messages for use by
    the GUI test runner in the NetBeans IDE.
    """

    def __init__(self, stream):
        TestResult.__init__(self)
        self.stream = stream
        self._start_time = None
        self._doctest_id = 1

    def getDescription(self, test):
        return str(test)

    def startTest(self, test):
        TestResult.startTest(self, test)
        self.stream.writeln("%%TEST_STARTED%% %s" % self.getDescription(test))
        self._start_time = time.time()

    def addSuccess(self, test):
        time_taken = time.time() - self._start_time
        TestResult.addSuccess(self, test)
        self.stream.writeln(
            "%%TEST_FINISHED%% time=%.6f %s test_path=%s"
            % (time_taken, self.getDescription(test), self.get_absolute_test_path(test))
        )

    def get_absolute_test_path(self, test):
        """
        Returns the absolute path of the current unittest case
        Written by chatGPT :)
        """
        # Get the current test method name
        test_method_name = test._testMethodName
        # Get the test case class name
        test_class_name = test.__class__.__name__
        # Get the test module path
        test_module_path = inspect.getfile(test.__class__)
        # Get the test method line number
        test_method_line = inspect.getsourcelines(getattr(test, test_method_name))[-1]
        # Construct the test case path
        # test_case_path = os.path.join(os.path.dirname(test_module_path), test_class_name + "." + test_method_name + ":" + str(test_method_line))
        # Convert the relative path to an absolute path
        # absolute_path = os.path.abspath(test_case_path)
        return "%s##%s" % (test_module_path, test_method_line)

    def _generate_stack(self, tb):
        # Default stack format:
        # stack = traceback.format_tb(tb)
        # stackstr = ""
        # for line in stack:
        #    stackstr += line
        # stackstr = stackstr.replace('\n', '%BR%')

        # More compact stack format
        stackstr = ""
        stack = []
        while tb:
            stack.append(tb.tb_frame)
            tb = tb.tb_next
        stack.reverse()
        for frame in stack:
            stackstr += "%s() in %s:%s%%BR%%" % (
                frame.f_code.co_name,
                frame.f_code.co_filename,
                frame.f_lineno,
            )

        return stackstr

    def addExpectedFailure(self, test, err):
        time_taken = time.time() - self._start_time
        TestResult.addExpectedFailure(self, test, err)

        (error, message, tb) = err
        stackstr = self._generate_stack(tb)

        message_str = str(message)
        if message_str.startswith("Failed doctest test for "):
            self._handle_doctest(time_taken, message_str, stackstr)
            return
        self.stream.writeln(
            "%%TEST_FINISHED%% time=%.6f %s test_path=%s"
            % (time_taken, self.getDescription(test), self.get_absolute_test_path(test))
        )

    def addUnexpectedSuccess(self, test):
        time_taken = time.time() - self._start_time
        TestResult.addUnexpectedSuccess(self, test)
        self.stream.writeln(
            "%%TEST_FAILED%% time=%.6f %s %s test_path=%s"
            % (
                time_taken,
                self.getDescription(test),
                "unexpected success",
                self.get_absolute_test_path(test),
            )
        )

    def _handle_doctest(self, time_taken, message_str, stackstr):
        for m in re.finditer(
            r"---------------------+\nFile \"(.+)\", line (.+), in (.*)\nFailed example:\n(.+)\nExpected:\n(.+)\n(Got:\n(.+)|Got nothing)\n",
            message_str,
            re.DOTALL,
        ):
            filename = m.group(1)
            lineno = m.group(2)
            testname = m.group(3)
            stackstr = "%s() in %s:%s%%BR%%" % (testname, filename, lineno)
            test_path = "%s:%s" % (filename, lineno)
            # HACK: Regexp in handler expected method (class) format
            # Plus, we need unique ids for each test so just synthesize one here
            testname = testname + " (" + str(self._doctest_id) + ")"
            self._doctest_id += 1
            expected = m.group(5).strip()
            got = "Nothing"
            if len(m.groups()) >= 7:
                got = m.group(7).strip()
            msg = "Expected " + expected + " but got " + got
            self.stream.writeln("%%TEST_STARTED%% %s" % testname)
            self.stream.writeln(
                "%%TEST_FAILED%% time=%.6f testname=%s message=%s location=%s test_path=%s"
                % (time_taken, testname, msg.replace("\n", "%BR%"), stackstr, test_path)
            )

    def addError(self, test, err):
        time_taken = time.time() - self._start_time
        TestResult.addError(self, test, err)
        (error, message, tb) = err
        stackstr = self._generate_stack(tb)
        self.stream.writeln(
            "%%TEST_ERROR%% time=%.6f testname=%s message=%s location=%s test_path=%s"
            % (
                time_taken,
                self.getDescription(test),
                str(message).replace("\n", "%BR%"),
                stackstr,
                self.get_absolute_test_path(test),
            )
        )

    def addFailure(self, test, err):
        time_taken = time.time() - self._start_time
        TestResult.addFailure(self, test, err)

        (error, message, tb) = err
        stackstr = self._generate_stack(tb)

        message_str = str(message)
        if message_str.startswith("Failed doctest test for "):
            self._handle_doctest(time_taken, message_str, stackstr)
            return
        self.stream.writeln(
            "%%TEST_FAILED%% time=%.6f testname=%s message=%s location=%s test_path=%s"
            % (
                time_taken,
                self.getDescription(test),
                message_str.replace("\n", "%BR%"),
                stackstr,
                self.get_absolute_test_path(test),
            )
        )

    def printErrors(self):
        # TestResult.printErrors(self)
        # if self.errors:
        #    self.stream.writeln("%%ERRORS%% %s" % self.errors)
        pass

    def printErrorList(self, flavour, errors):
        pass

    def addSkip(self, test, reason):
        time_taken = time.time() - self._start_time
        TestResult.addSkip(self, test, reason)
        self.stream.writeln(
            "%%TEST_SKIPPED%% time=%.6f testname=%s reason=%s, test_path=%s"
            % (
                time_taken,
                self.getDescription(test),
                reason,
                self.get_absolute_test_path(test),
            )
        )

    def addSubTest(self, test, subtest, err):
        time_taken = time.time() - self._start_time
        TestResult.addSubTest(self, test, subtest, err)

        if err:
            (error, message, tb) = err
            stackstr = self._generate_stack(tb)

            message_str = str(message)
            if message_str.startswith("Failed doctest test for "):
                self._handle_doctest(time_taken, message_str, stackstr)
                return
            self.stream.writeln(
                "%%TEST_FAILED%% time=%.6f subtest=%s message=%s location=%s test_path=%s"
                % (
                    time_taken,
                    self.getDescription(subtest),
                    message_str.replace("\n", "%BR%"),
                    stackstr,
                    self.get_absolute_test_path(test),
                )
            )
            return
        self.stream.writeln(
            "%%TEST_FINISHED%% time=%.6f subtest=%s test_path=%s"
            % (
                time_taken,
                self.getDescription(subtest),
                self.get_absolute_test_path(test),
            )
        )


class _NetBeansTestRunner:
    """A test runner class that displays results in textual form.

    It prints out the names of tests as they are run, errors as they
    occur, and a summary of the results at the end of the test run.
    """

    def __init__(self, stream=sys.stdout):
        self.stream = _NbWritelnDecorator(stream)

    def _makeResult(self):
        return _NbTextTestResult(self.stream)

    def _get_suite_name(self, test):
        if hasattr(test, "_tests"):
            name_set = set()
            for o in test._tests:
                try:
                    for s in o._tests:
                        name_set.add(s.__class__)
                except Exception as e:
                    name_set.add(o._testMethodName)
            name = ""
            for s in name_set:
                if len(name) > 0:
                    name += ","
                # name += s.__module__ + '.' + s.__name__
                if isinstance(s, str):
                    name += s
                else:
                    name += s.__name__
            return name
        else:
            class_ = test.__class__
            classname = class_.__module__ + "." + class_.__name__
            return classname

    def run(self, test):
        "Run the given test case or test suite."
        suite_name = self._get_suite_name(test)
        self.stream.writeln("%%SUITE_STARTING%% %s" % suite_name)
        result = self._makeResult()
        startTime = time.time()
        test(result)
        stopTime = time.time()
        timeTaken = stopTime - startTime
        result.printErrors()
        if not result.wasSuccessful():
            failed, errored = map(len, (result.failures, result.errors))
            if failed:
                self.stream.writeln("%%SUITE_FAILURES%% %d" % failed)
            if errored:
                self.stream.writeln("%%SUITE_ERRORS%% %d" % errored)
        else:
            self.stream.writeln("%SUITE_SUCCESS%")
        self.stream.writeln("%%SUITE_FINISHED%% time=%.4f" % timeTaken)
        return result


##############################################################################
# Driver for running tests from NetBeans
##############################################################################
if __name__ == "__main__":
    import doctest
    import unittest

    parser = OptionParser(
        usage="%prog <[--method <name> ]--file|--directory>  <files/directories...>",
        version="%prog 1.0",
    )
    parser.add_option(
        "-f", "--file", action="store_true", dest="filename", help="Test the given file"
    )
    parser.add_option(
        "-m",
        "--method",
        action="store",
        type="string",
        dest="method",
        help="Test the given method",
    )
    parser.add_option(
        "-d",
        "--directory",
        action="store_true",
        dest="directory",
        help="Test the given directory",
    )
    parser.add_option(
        "-p",
        "--pattern",
        action="store_true",
        dest="pattern",
        help="Test files pattern",
    )
    (options, args) = parser.parse_args()
    if len(args) == 0:
        parser.error("Don't forget to specify files/directories")
    if options.method and not options.filename:
        parser.error("must specify --file if you specify --method")
    if options.filename and options.directory:
        parser.error("--directory and --file are mutually exclusive")
    if not options.filename and not options.directory:
        parser.error(
            "You must specify at least one of --file, --method and --directory"
        )
    if options.directory and not options.pattern:
        parser.error("--directory and --pattern must be specified")

    if options.filename or options.method:
        if len(args) > 1:
            parser.error("You can only specify one file with --file")
        # Fix for "ImportError: Import by filename is not supported." from 2.6
        # Craig Milling <ctmilling@gmail.com> 2010-02-16
        dir_name = os.path.dirname(args[0])
        if not dir_name in sys.path:
            sys.path.append(dir_name)
        module_name = os.path.basename(args[0])
        if module_name.endswith(".py"):
            module_name = module_name[:-3]
        module = __import__(module_name, globals(), locals(), module_name)
        if options.method:
            suite = unittest.TestLoader().loadTestsFromName(options.method, module)
        else:
            suite = unittest.TestLoader().loadTestsFromModule(module)
            # Doctest
            try:
                suite.addTest(doctest.DocTestSuite(module))
            except ValueError as e:
                # doctest will raise ValueError(module, "has no tests")
                # and we're trying this on random modules: No loud complaints!
                pass

    else:
        assert options.directory
        test_modules = []

        for dir in args[0].split(" "):
            relative_start = len(dir)
            if dir[relative_start - 1] != os.sep:
                relative_start += 1
            module_names = []
            for root, dirs, files in os.walk(dir):
                for file_name in files:
                    extension = os.path.splitext(file_name)[-1]
                    if extension == ".py":
                        relative = root[relative_start:]
                        pkg = relative.replace(os.sep, ".")
                        base = os.path.splitext(file_name)[0:-1][0]
                        if base == "__init__":
                            continue
                        if len(pkg) > 0:
                            module_name = pkg + "." + base
                        else:
                            module_name = base
                        module_names.append(module_name)
            for module_name in module_names:
                try:
                    module = __import__(module_name, globals(), locals(), module_name)
                    test_modules.append(module)
                except Exception as e:
                    # No complaints - just test the files we can (user may have run
                    # test project on an unfinished project where not all files are valid)
                    pass
        if test_modules:
            suite = unittest.TestSuite(
                map(unittest.defaultTestLoader.loadTestsFromModule, test_modules)
            )
        else:
            suite = unittest.TestLoader().discover(start_dir=dir, pattern=args[1])
        # Doctest
        for module in test_modules:
            try:
                suite.addTest(doctest.DocTestSuite(module))
            except ValueError as e:
                # doctest will raise ValueError(module, "has no tests")
                # and we're trying this on random modules: No loud complaints!
                pass

    # Run all the tests
    _NetBeansTestRunner().run(suite)
