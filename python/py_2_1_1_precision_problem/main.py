"""
Demonstrates common pitfalls with floating-point arithmetic in Java,
including precision errors and catastrophic cancellation.
Tested with Python 3.13.
"""
import random


def escalation_addition():
    """
    Case 1: Repeated addition
    Demonstrates precision errors caused by repeated addition.
    """
    result = 0.0
    for i in range(1000000):
        result += 0.1

    expected = 100000.0
    actual = result
    error = abs(expected - actual)

    print(f"Expected: {expected}")
    print(f"Actual:   {actual}")
    print(f"Error:    {error}")
    print(f"Error %:  {(error / expected) * 100:.10f}%\n")


def sum_different_magnitudes():
    """
    Case 2: Summation with different magnitudes
    Demonstrates catastrophic cancellation.
    """
    large = 1e16
    total = large

    # Add small values to large number
    for i in range(1000):
        total += 1.0
    total -= large

    expected = 1000.0
    print(f"Adding small values to large number:")
    print(f"Expected: {expected}")
    print(f"Actual:   {total}")
    print(f"Error:    {abs(expected - total)}\n")


def non_associativity():
    """
    Case 3: Non-associativity of floating point
    Demonstrates non-associativity of floating-point arithmetic.
    """
    a = 0.1
    b = 0.2
    c = 0.3

    # Different grouping orders
    result1 = (a + b) + c
    result2 = a + (b + c)

    print(f"Non-associativity: (0.1 + 0.2) + 0.3 vs 0.1 + (0.2 + 0.3)")
    print(f"(a + b) + c = {result1:.20f}")
    print(f"a + (b + c) = {result2:.20f}")
    print(f"Difference:   {abs(result1 - result2):.20e}\n")


def iterative_computation():
    """
    Case 4: Iterative computation with varying inputs
    Demonstrates order-dependent summation errors.
    """
    random.seed(42)
    values = [random.random() * 0.01 for _ in range(1000)]

    # Sum forward
    sum_forward = 0.0
    for v in values:
        sum_forward += v

    # Sum backward
    sum_backward = 0.0
    for v in reversed(values):
        sum_backward += v

    print(f"Order-dependent summation:")
    print(f"Forward sum:  {sum_forward:.15f}")
    print(f"Backward sum: {sum_backward:.15f}")
    print(f"Difference:   {abs(sum_forward - sum_backward):.15e}\n")


if __name__ == "__main__":
    print("-- Example: Python floating point precision error escalation ---\n")
    escalation_addition()
    sum_different_magnitudes()
    non_associativity()
    iterative_computation()