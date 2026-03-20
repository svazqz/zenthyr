import { useState, useEffect } from 'react';

interface Message {
  type: string;
  data: any;
}

function App() {
  const [count, setCount] = useState(0);

  useEffect(() => {
    const handleMessage = (event: CustomEvent<Message>) => {
      // Handle incoming messages
      setCount(event.detail.data.count || count);
    };

    window.addEventListener('clojure-message', handleMessage as EventListener);

    return () => {
      window.removeEventListener('clojure-message', handleMessage as EventListener);
    };
  }, [count]);

  const handleIncrement = async () => {
    const {data: { result }} = await (window as any).zenthyr.invoke({
      action: 'counter',
      data: {
        type: 'increment'
      }
    });
    setCount(result);
  };

  const handleDecrement = async () => {
    const {data: { result }} = await (window as any).zenthyr.invoke({
      action: 'counter',
      data: {
        type: 'decrement'
      }
    });
    setCount(result);
  };

  return (
    <div className='counter'>
      <div className='counter-value'>{count}</div>
      <div className='counter-controls'>
        <button type='button' className='decrement' onClick={handleDecrement}>
          Decrement
        </button>
        <button type='button' className='increment' onClick={handleIncrement}>
          Increment
        </button>
      </div>
    </div>
  );
}

export default App;
