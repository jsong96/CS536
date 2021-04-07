
#include <stdio.h>

using namespace std;

int main()

{

  int a1 = 5, a2 = 8;

  cout << "a1 = " << a1 << "\ta2 = " << a2 << '\n';

  // legal call

  Func(a1, a2);

  cout << "a1 = " << a1 << "\ta2 = " << a2 << '\n';

  // legal call

  Func(a1, 20);

  cout << "a1 = " << a1 << "\ta2 = " << a2 << '\n';

  return 0;
}

void Func(int &x, int y)

{

  x = x * 2;

  y = y + 2;

  cout << "x = " << x << '\n';

  cout << "y = " << y << '\n';
}