

# Created on : ${date}, ${time}
# Author     : ${user}

import unittest


class ${className}(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        # add instructions

        pass

    @classmethod
    def tearDownClass(cls):
        # add instructions

        pass

    def setUp(self):
        # add instructions

        pass

    def tearDown(self):
        # add instructions

        pass

    @unittest.expectedFailure
    def ${methodName}(self):
        self.assertEqual(True, False, 'This test is not correct')  # add assertion here


if __name__ == "__main__":
    unittest.main()
