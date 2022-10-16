from diffimg import diff
from PIL import Image
import sys

Image.MAX_IMAGE_PIXELS = None

def main():
    expected = sys.argv[1]
    actual = sys.argv[2]
    print("Expected File: " + expected)
    print("Actual File: " + actual)
    difference = diff(expected, actual)
    print("Difference in percent : " + str(difference) + "%")

if __name__ == "__main__":
    main()