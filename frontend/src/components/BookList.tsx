import React, { useState, useEffect } from 'react';

//define structure of the book JSON
interface Book{ 
    title: string;
    level: 'N5' | 'N4' | 'N3' | 'N2' | 'N1';
}

const BookList: React.FC = () => { 
    const [books, setBooks] = useState<Book[]>([]);
    const [aniBooks, setAniBooks] = useState<Book[]>([]);
    const [filteredBooks, setFilteredBooks] = useState<Book[]>([]);
    const [filter, setFilter] = useState<string>('All');

    useEffect(() => {
        //fetch json data from honto file
        fetch('http://localhost:8080/api/books')
            .then(response => response.json())
            .then(data => {
                const allBooks = Object.keys(data).flatMap(level =>
                    data[level].map((title: string) => ({ title, level }))
                );
                setBooks(allBooks);
                setFilteredBooks(allBooks);
            })
            .catch(error => console.error("Error loading the book data: ", error));

        //fetch json data from anionline file
        fetch('http://localhost:8080/api/ani-books')
        .then(response => response.json())
        .then(data => {
            const allAniBooks = Object.keys(data).flatMap(level =>
                data[level].map((title: string) => ({ title, level }))
            );
            setAniBooks(allAniBooks);
        })
        .catch(error => console.error("Error loading the Ani books data: ", error));

        }, []);


  // Update the filtered books whenever the filter changes
  useEffect(() => {
    const allFilteredBooks = books.concat(aniBooks);
    setFilteredBooks(
        filter === 'All' ? allFilteredBooks : allFilteredBooks.filter(book => book.level === filter)
    );
  }, [filter, books, aniBooks]);

    return (
        <div className='container mx-auto p-4'>
            <h1 className='text-2xl font-bold mb-4'>Shiru</h1>
            <div className='mb-4'>
                {['N5', 'N4', 'N3', 'N2', 'N1'].map((level) => (
                    <button
                        key={level}
                        className={`p-2 border ${filter === level ? 'bg-blue-500 text-white' :'bg-white text-black'}`}
                        onClick={() => setFilter(level)}
                    >
                        {level}
                    </button>
                ))}
        </div>
        <ul className='list-disc pl-5'>
            {filteredBooks.map((book, index) => (
                <li key={index}>
                    {book.title} ({book.level})
                </li>
            ))}
        </ul>
    </div>
    );
};

export default BookList;

